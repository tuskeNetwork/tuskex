/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package tuskex.core.support.dispute.mediation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import tuskex.common.Timer;
import tuskex.common.UserThread;
import tuskex.common.app.Version;
import tuskex.common.config.Config;
import tuskex.common.crypto.KeyRing;
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.common.handlers.ResultHandler;
import tuskex.core.api.TskConnectionService;
import tuskex.core.api.CoreNotificationService;
import tuskex.core.locale.Res;
import tuskex.core.offer.OpenOffer;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.support.SupportType;
import tuskex.core.support.dispute.Dispute;
import tuskex.core.support.dispute.DisputeManager;
import tuskex.core.support.dispute.DisputeResult;
import tuskex.core.support.dispute.messages.DisputeClosedMessage;
import tuskex.core.support.dispute.messages.DisputeOpenedMessage;
import tuskex.core.support.messages.ChatMessage;
import tuskex.core.support.messages.SupportMessage;
import tuskex.core.trade.ClosedTradableManager;
import tuskex.core.trade.Trade;
import tuskex.core.trade.TradeManager;
import tuskex.core.trade.protocol.DisputeProtocol;
import tuskex.core.trade.protocol.ProcessModel;
import tuskex.core.tsk.wallet.TradeWalletService;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.network.p2p.AckMessageSourceType;
import tuskex.network.p2p.NodeAddress;
import tuskex.network.p2p.P2PService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Singleton
public final class MediationManager extends DisputeManager<MediationDisputeList> {

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public MediationManager(P2PService p2PService,
                            TradeWalletService tradeWalletService,
                            TskWalletService walletService,
                            TskConnectionService tskConnectionService,
                            CoreNotificationService notificationService,
                            TradeManager tradeManager,
                            ClosedTradableManager closedTradableManager,
                            OpenOfferManager openOfferManager,
                            KeyRing keyRing,
                            MediationDisputeListService mediationDisputeListService,
                            Config config,
                            PriceFeedService priceFeedService) {
        super(p2PService, tradeWalletService, walletService, tskConnectionService, notificationService, tradeManager, closedTradableManager,
                openOfferManager, keyRing, mediationDisputeListService, config, priceFeedService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Implement template methods
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public SupportType getSupportType() {
        return SupportType.MEDIATION;
    }

    @Override
    public void onSupportMessage(SupportMessage message) {
        if (canProcessMessage(message)) {
            log.info("Received {} with tradeId {} and uid {}",
                    message.getClass().getSimpleName(), message.getTradeId(), message.getUid());

            if (message instanceof DisputeOpenedMessage) {
                handleDisputeOpenedMessage((DisputeOpenedMessage) message);
            } else if (message instanceof ChatMessage) {
                handleChatMessage((ChatMessage) message);
            } else if (message instanceof DisputeClosedMessage) {
                handleDisputeClosedMessage((DisputeClosedMessage) message);
            } else {
                log.warn("Unsupported message at dispatchMessage. message={}", message);
            }
        }
    }

    @Override
    protected AckMessageSourceType getAckMessageSourceType() {
        return AckMessageSourceType.MEDIATION_MESSAGE;
    }

    @Override
    public void cleanupDisputes() {
        disputeListService.cleanupDisputes(tradeId -> {
            tradeManager.getOpenTrade(tradeId).filter(trade -> trade.getPayoutTx() != null)
                    .ifPresent(trade -> {
                        tradeManager.closeDisputedTrade(tradeId, Trade.DisputeState.MEDIATION_CLOSED);
                    });
        });
    }

    @Override
    protected String getDisputeInfo(Dispute dispute) {
        String role = Res.get("shared.mediator").toLowerCase();
        String link = "https://docs.tuskex.exchange/trading-rules.html#mediation";
        return Res.get("support.initialInfo", role, role, link);
    }

    @Override
    protected String getDisputeIntroForPeer(String disputeInfo) {
        return Res.get("support.peerOpenedDisputeForMediation", disputeInfo, Version.VERSION);
    }

    @Override
    protected String getDisputeIntroForDisputeCreator(String disputeInfo) {
        return Res.get("support.youOpenedDisputeForMediation", disputeInfo, Version.VERSION);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Message handler
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    // We get that message at both peers. The dispute object is in context of the trader
    public void handleDisputeClosedMessage(DisputeClosedMessage disputeResultMessage) {
        DisputeResult disputeResult = disputeResultMessage.getDisputeResult();
        String tradeId = disputeResult.getTradeId();
        ChatMessage chatMessage = disputeResult.getChatMessage();
        checkNotNull(chatMessage, "chatMessage must not be null");
        Optional<Dispute> disputeOptional = findDispute(disputeResult);
        String uid = disputeResultMessage.getUid();
        if (!disputeOptional.isPresent()) {
            log.warn("We got a dispute result msg but we don't have a matching dispute. " +
                    "That might happen when we get the disputeResultMessage before the dispute was created. " +
                    "We try again after 2 sec. to apply the disputeResultMessage. TradeId = " + tradeId);
            if (!delayMsgMap.containsKey(uid)) {
                // We delay 2 sec. to be sure the comm. msg gets added first
                Timer timer = UserThread.runAfter(() -> handleDisputeClosedMessage(disputeResultMessage), 2);
                delayMsgMap.put(uid, timer);
            } else {
                log.warn("We got a dispute result msg after we already repeated to apply the message after a delay. " +
                        "That should never happen. TradeId = " + tradeId);
            }
            return;
        }

        Dispute dispute = disputeOptional.get();
        cleanupRetryMap(uid);
        if (!dispute.getChatMessages().contains(chatMessage)) {
            dispute.addAndPersistChatMessage(chatMessage);
        } else {
            log.warn("We got a dispute mail msg that we have already stored. TradeId = " + chatMessage.getTradeId());
        }
        dispute.setIsClosed();

        dispute.setDisputeResult(disputeResult);

        Optional<Trade> tradeOptional = tradeManager.getOpenTrade(tradeId);
        if (tradeOptional.isPresent()) {
            Trade trade = tradeOptional.get();
            if (trade.getDisputeState() == Trade.DisputeState.MEDIATION_REQUESTED ||
                    trade.getDisputeState() == Trade.DisputeState.MEDIATION_STARTED_BY_PEER) {
                trade.getProcessModel().setBuyerPayoutAmountFromMediation(disputeResult.getBuyerPayoutAmountBeforeCost().longValueExact());
                trade.getProcessModel().setSellerPayoutAmountFromMediation(disputeResult.getSellerPayoutAmountBeforeCost().longValueExact());

                trade.setDisputeState(Trade.DisputeState.MEDIATION_CLOSED);

                tradeManager.requestPersistence();
            }
        } else {
            Optional<OpenOffer> openOfferOptional = openOfferManager.getOpenOfferById(tradeId);
            openOfferOptional.ifPresent(openOffer -> openOfferManager.closeOpenOffer(openOffer.getOffer()));
        }
        sendAckMessage(chatMessage, dispute.getAgentPubKeyRing(), true, null);

        requestPersistence();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    public NodeAddress getAgentNodeAddress(Dispute dispute) {
        return dispute.getContract().getArbitratorNodeAddress();  // TODO (woodser): mediator becomes and replaces current arbitrator?
    }

    public void onAcceptMediationResult(Trade trade,
                                        ResultHandler resultHandler,
                                        ErrorMessageHandler errorMessageHandler) {
        String tradeId = trade.getId();
        Optional<Dispute> optionalDispute = findDispute(tradeId);
        checkArgument(optionalDispute.isPresent(), "dispute must be present");
        DisputeResult disputeResult = optionalDispute.get().getDisputeResultProperty().get();
        BigInteger buyerPayoutAmount = disputeResult.getBuyerPayoutAmountBeforeCost();
        BigInteger sellerPayoutAmount = disputeResult.getSellerPayoutAmountBeforeCost();
        ProcessModel processModel = trade.getProcessModel();
        processModel.setBuyerPayoutAmountFromMediation(buyerPayoutAmount.longValueExact());
        processModel.setSellerPayoutAmountFromMediation(sellerPayoutAmount.longValueExact());
        DisputeProtocol tradeProtocol = (DisputeProtocol) tradeManager.getTradeProtocol(trade);

        trade.setMediationResultState(MediationResultState.MEDIATION_RESULT_ACCEPTED);
        tradeManager.requestPersistence();

        // If we have not got yet the peers signature we sign and send to the peer our signature.
        // Otherwise we sign and complete with the peers signature the payout tx.
        if (trade.getTradePeer().getMediatedPayoutTxSignature() == null) {
            tradeProtocol.onAcceptMediationResult(() -> {
                if (trade.getPayoutTx() != null) {
                    tradeManager.closeDisputedTrade(tradeId, Trade.DisputeState.MEDIATION_CLOSED);
                }
                resultHandler.handleResult();
            }, errorMessageHandler);
        } else {
            tradeProtocol.onFinalizeMediationResultPayout(() -> {
                if (trade.getPayoutTx() != null) {
                    tradeManager.closeDisputedTrade(tradeId, Trade.DisputeState.MEDIATION_CLOSED);
                }
                resultHandler.handleResult();
            }, errorMessageHandler);
        }
    }

    public void rejectMediationResult(Trade trade) {
        trade.setMediationResultState(MediationResultState.MEDIATION_RESULT_REJECTED);
        tradeManager.requestPersistence();
    }
}
