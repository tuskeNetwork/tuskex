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

import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.offer.OfferDirection;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.trade.TakerTrade;
import tuskex.core.trade.Trade;
import tuskex.core.trade.protocol.TradeProtocol;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.core.tsk.wallet.TskWalletService;
import lombok.extern.slf4j.Slf4j;
import monero.wallet.model.MoneroTxWallet;

import java.math.BigInteger;

@Slf4j
public class TakerReserveTradeFunds extends TradeTask {

    public TakerReserveTradeFunds(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // taker trade expected
            if (!(trade instanceof TakerTrade)) {
                throw new RuntimeException("Expected taker trade but was " + trade.getClass().getSimpleName() + " " + trade.getShortId() + ". That should never happen.");
            }

            // create reserve tx
            MoneroTxWallet reserveTx = null;
            synchronized (TskWalletService.WALLET_LOCK) {

                // check for timeout
                if (isTimedOut()) throw new RuntimeException("Trade protocol has timed out while getting lock to create reserve tx, tradeId=" + trade.getShortId());
                trade.startProtocolTimeout();

                // collect relevant info
                BigInteger penaltyFee = TuskexUtils.multiply(trade.getAmount(), trade.getOffer().getPenaltyFeePct());
                BigInteger takerFee = trade.getTakerFee();
                BigInteger sendAmount = trade.getOffer().getDirection() == OfferDirection.BUY ? trade.getAmount() : BigInteger.ZERO;
                BigInteger securityDeposit = trade.getOffer().getDirection() == OfferDirection.BUY ? trade.getSellerSecurityDepositBeforeMiningFee() : trade.getBuyerSecurityDepositBeforeMiningFee();
                String returnAddress = trade.getTskWalletService().getOrCreateAddressEntry(trade.getOffer().getId(), TskAddressEntry.Context.TRADE_PAYOUT).getAddressString();

                // attempt creating reserve tx
                try {
                    synchronized (TuskexUtils.getWalletFunctionLock()) {
                        for (int i = 0; i < TradeProtocol.MAX_ATTEMPTS; i++) {
                            try {
                                reserveTx = model.getTskWalletService().createReserveTx(penaltyFee, takerFee, sendAmount, securityDeposit, returnAddress, false, null);
                            } catch (Exception e) {
                                log.warn("Error creating reserve tx, attempt={}/{}, tradeId={}, error={}", i + 1, TradeProtocol.MAX_ATTEMPTS, trade.getShortId(), e.getMessage());
                                if (i == TradeProtocol.MAX_ATTEMPTS - 1) throw e;
                                TuskexUtils.waitFor(TradeProtocol.REPROCESS_DELAY_MS); // wait before retrying
                            }
            
                            // check for timeout
                            if (isTimedOut()) throw new RuntimeException("Trade protocol has timed out while creating reserve tx, tradeId=" + trade.getShortId());
                            if (reserveTx != null) break;
                        }
                    }
                } catch (Exception e) {

                    // reset state with wallet lock
                    model.getTskWalletService().resetAddressEntriesForTrade(trade.getId());
                    if (reserveTx != null) {
                        model.getTskWalletService().thawOutputs(TuskexUtils.getInputKeyImages(reserveTx));
                        trade.getSelf().setReserveTxKeyImages(null);
                    }

                    throw e;
                }


                // reset protocol timeout
                trade.startProtocolTimeout();

                // update trade state
                trade.getTaker().setReserveTxHash(reserveTx.getHash());
                trade.getTaker().setReserveTxHex(reserveTx.getFullHex());
                trade.getTaker().setReserveTxKey(reserveTx.getKey());
                trade.getTaker().setReserveTxKeyImages(TuskexUtils.getInputKeyImages(reserveTx));
            }

            // save process state
            processModel.setReserveTx(reserveTx); // TODO: remove this? how is it used?
            processModel.getTradeManager().requestPersistence();
            trade.addInitProgressStep();
            complete();
        } catch (Throwable t) {
            trade.setErrorMessage("An error occurred.\n" +
                "Error message:\n"
                + t.getMessage());
            failed(t);
        }
    }

    private boolean isTimedOut() {
        return !processModel.getTradeManager().hasOpenTrade(trade);
    }
}
