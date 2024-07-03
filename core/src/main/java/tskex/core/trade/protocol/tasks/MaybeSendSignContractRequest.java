/*
 * This file is part of Tuskex.
 *
 * Tuskex is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Tuskex is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Tuskex. If not, see <http://www.gnu.org/licenses/>.
 */

package tuskex.core.trade.protocol.tasks;

import tuskex.common.app.Version;
import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.trade.ArbitratorTrade;
import tuskex.core.trade.BuyerTrade;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.trade.MakerTrade;
import tuskex.core.trade.Trade;
import tuskex.core.trade.Trade.State;
import tuskex.core.trade.messages.SignContractRequest;
import tuskex.core.trade.protocol.TradeProtocol;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.network.p2p.SendDirectMessageListener;
import lombok.extern.slf4j.Slf4j;
import monero.wallet.model.MoneroTxWallet;

import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

// TODO (woodser): separate classes for deposit tx creation and contract request, or combine into ProcessInitMultisigRequest
@Slf4j
public class MaybeSendSignContractRequest extends TradeTask {

    private boolean ack1 = false; // TODO (woodser) these represent onArrived(), not the ack
    private boolean ack2 = false;

    @SuppressWarnings({"unused"})
    public MaybeSendSignContractRequest(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // skip if arbitrator
            if (trade instanceof ArbitratorTrade) {
                complete();
                return;
            }

            // skip if multisig wallet not complete
            if (processModel.getMultisigAddress() == null) {
                complete();
                return;
            }

            // skip if deposit tx already created
            if (trade.getSelf().getDepositTx() != null) {
                complete();
                return;
            }

            // initialize progress steps
            trade.addInitProgressStep();

            // create deposit tx and freeze inputs
            MoneroTxWallet depositTx = null;
            synchronized (TskWalletService.WALLET_LOCK) {

                // check for timeout
                if (isTimedOut()) throw new RuntimeException("Trade protocol has timed out while getting lock to create deposit tx, tradeId=" + trade.getShortId());
                trade.startProtocolTimeout();

                // collect relevant info
                Integer subaddressIndex = null;
                boolean reserveExactAmount = false;
                if (trade instanceof MakerTrade) {
                    reserveExactAmount = processModel.getOpenOfferManager().getOpenOfferById(trade.getId()).get().isReserveExactAmount();
                    if (reserveExactAmount) subaddressIndex = model.getTskWalletService().getAddressEntry(trade.getId(), TskAddressEntry.Context.OFFER_FUNDING).get().getSubaddressIndex();
                }

                // thaw reserved outputs
                if (trade.getSelf().getReserveTxKeyImages() != null) {
                    trade.getTskWalletService().thawOutputs(trade.getSelf().getReserveTxKeyImages());
                }

                // attempt creating deposit tx
                try {
                    synchronized (TuskexUtils.getWalletFunctionLock()) {
                        for (int i = 0; i < TradeProtocol.MAX_ATTEMPTS; i++) {
                            try {
                                depositTx = trade.getTskWalletService().createDepositTx(trade, reserveExactAmount, subaddressIndex);
                            } catch (Exception e) {
                                log.warn("Error creating deposit tx, attempt={}/{}, tradeId={}, error={}", i + 1, TradeProtocol.MAX_ATTEMPTS, trade.getShortId(), e.getMessage());
                                if (i == TradeProtocol.MAX_ATTEMPTS - 1) throw e;
                                TuskexUtils.waitFor(TradeProtocol.REPROCESS_DELAY_MS); // wait before retrying
                            }
            
                            // check for timeout
                            if (isTimedOut()) throw new RuntimeException("Trade protocol has timed out while creating deposit tx, tradeId=" + trade.getShortId());
                            if (depositTx != null) break;
                        }
                    }
                } catch (Exception e) {

                    // thaw deposit inputs
                    if (depositTx != null) {
                        trade.getTskWalletService().thawOutputs(TuskexUtils.getInputKeyImages(depositTx));
                        trade.getSelf().setReserveTxKeyImages(null);
                    }

                    // re-freeze maker offer inputs
                    if (trade instanceof MakerTrade) {
                        trade.getTskWalletService().freezeOutputs(trade.getOffer().getOfferPayload().getReserveTxKeyImages());
                    }

                    throw e;
                }

                // reset protocol timeout
                trade.addInitProgressStep();

                // update trade state
                BigInteger securityDeposit = trade instanceof BuyerTrade ? trade.getBuyerSecurityDepositBeforeMiningFee() : trade.getSellerSecurityDepositBeforeMiningFee();
                trade.getSelf().setSecurityDeposit(securityDeposit.subtract(depositTx.getFee()));
                trade.getSelf().setDepositTx(depositTx);
                trade.getSelf().setDepositTxHash(depositTx.getHash());
                trade.getSelf().setDepositTxFee(depositTx.getFee());
                trade.getSelf().setReserveTxKeyImages(TuskexUtils.getInputKeyImages(depositTx));
                trade.getSelf().setPayoutAddressString(trade.getTskWalletService().getOrCreateAddressEntry(trade.getOffer().getId(), TskAddressEntry.Context.TRADE_PAYOUT).getAddressString()); // TODO (woodser): allow custom payout address?
                trade.getSelf().setPaymentAccountPayload(trade.getProcessModel().getPaymentAccountPayload(trade.getSelf().getPaymentAccountId()));
            }

            // maker signs deposit hash nonce to avoid challenge protocol
            byte[] sig = null;
            if (trade instanceof MakerTrade) {
                sig = TuskexUtils.sign(processModel.getP2PService().getKeyRing(), depositTx.getHash());
            }

            // create request for peer and arbitrator to sign contract
            SignContractRequest request = new SignContractRequest(
                    trade.getOffer().getId(),
                    UUID.randomUUID().toString(),
                    Version.getP2PMessageVersion(),
                    new Date().getTime(),
                    trade.getProcessModel().getAccountId(),
                    trade.getSelf().getPaymentAccountPayload().getHash(),
                    trade.getSelf().getPayoutAddressString(),
                    depositTx.getHash(),
                    sig);

            // send request to trading peer
            processModel.getP2PService().sendEncryptedDirectMessage(trade.getTradePeer().getNodeAddress(), trade.getTradePeer().getPubKeyRing(), request, new SendDirectMessageListener() {
                @Override
                public void onArrived() {
                    log.info("{} arrived: trading peer={}; offerId={}; uid={}", request.getClass().getSimpleName(), trade.getTradePeer().getNodeAddress(), trade.getId());
                    ack1 = true;
                    if (ack1 && ack2) completeAux();
                }
                @Override
                public void onFault(String errorMessage) {
                    log.error("Sending {} failed: uid={}; peer={}; error={}", request.getClass().getSimpleName(), trade.getTradePeer().getNodeAddress(), trade.getId(), errorMessage);
                    appendToErrorMessage("Sending message failed: message=" + request + "\nerrorMessage=" + errorMessage);
                    failed();
                }
            });

            // send request to arbitrator
            processModel.getP2PService().sendEncryptedDirectMessage(trade.getArbitrator().getNodeAddress(), trade.getArbitrator().getPubKeyRing(), request, new SendDirectMessageListener() {
                @Override
                public void onArrived() {
                    log.info("{} arrived: trading peer={}; offerId={}; uid={}", request.getClass().getSimpleName(), trade.getArbitrator().getNodeAddress(), trade.getId());
                    ack2 = true;
                    if (ack1 && ack2) completeAux();
                }
                @Override
                public void onFault(String errorMessage) {
                    log.error("Sending {} failed: uid={}; peer={}; error={}", request.getClass().getSimpleName(), trade.getArbitrator().getNodeAddress(), trade.getId(), errorMessage);
                    appendToErrorMessage("Sending message failed: message=" + request + "\nerrorMessage=" + errorMessage);
                    failed();
                }
            });
        } catch (Throwable t) {
          failed(t);
        }
    }

    private void completeAux() {
        trade.setState(State.CONTRACT_SIGNATURE_REQUESTED);
        trade.addInitProgressStep();
        processModel.getTradeManager().requestPersistence();
        complete();
    }

    private boolean isTimedOut() {
        return !processModel.getTradeManager().hasOpenTrade(trade);
    }
}
