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
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.common.handlers.ResultHandler;
import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.offer.availability.DisputeAgentSelection;
import tuskex.core.support.dispute.arbitration.arbitrator.Arbitrator;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.InitTradeRequest;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.network.p2p.NodeAddress;
import tuskex.network.p2p.SendDirectMessageListener;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MakerSendInitTradeRequestToArbitrator extends TradeTask {

    @SuppressWarnings({"unused"})
    public MakerSendInitTradeRequestToArbitrator(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // get least used arbitrator
            Arbitrator leastUsedArbitrator = DisputeAgentSelection.getLeastUsedArbitrator(processModel.getTradeStatisticsManager(), processModel.getArbitratorManager());
            if (leastUsedArbitrator == null) {
                failed("Could not get least used arbitrator to send " + InitTradeRequest.class.getSimpleName() + " for offer " + trade.getId());
                return;
            }

            // send request to least used arbitrators until success
            sendInitTradeRequests(leastUsedArbitrator.getNodeAddress(), new HashSet<NodeAddress>(), () -> {
                trade.addInitProgressStep();
                complete();
            }, (errorMessage) -> {
                log.warn("Cannot initialize trade with arbitrators: " + errorMessage);
                failed(errorMessage);
            });
        } catch (Throwable t) {
          failed(t);
        }
    }

    private void sendInitTradeRequests(NodeAddress arbitratorNodeAddress, Set<NodeAddress> excludedArbitrators, ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        sendInitTradeRequest(arbitratorNodeAddress, new SendDirectMessageListener() {
            @Override
            public void onArrived() {
                log.info("{} arrived at arbitrator: offerId={}", InitTradeRequest.class.getSimpleName(), trade.getId());

                // check if trade still exists
                if (!processModel.getTradeManager().hasOpenTrade(trade)) {
                    errorMessageHandler.handleErrorMessage("Trade protocol no longer exists, tradeId=" + trade.getId());
                    return;
                }
                resultHandler.handleResult();
            }

            // if unavailable, try alternative arbitrator
            @Override
            public void onFault(String errorMessage) {
                log.warn("Arbitrator unavailable: address={}, error={}", arbitratorNodeAddress, errorMessage);
                excludedArbitrators.add(arbitratorNodeAddress);

                // check if trade still exists
                if (!processModel.getTradeManager().hasOpenTrade(trade)) {
                    errorMessageHandler.handleErrorMessage("Trade protocol no longer exists, tradeId=" + trade.getId());
                    return;
                }

                Arbitrator altArbitrator = DisputeAgentSelection.getLeastUsedArbitrator(processModel.getTradeStatisticsManager(), processModel.getArbitratorManager(), excludedArbitrators);
                if (altArbitrator == null) {
                    errorMessageHandler.handleErrorMessage("Cannot take offer because no arbitrators are available");
                    return;
                }
                log.info("Using alternative arbitrator {}", altArbitrator.getNodeAddress());
                sendInitTradeRequests(altArbitrator.getNodeAddress(), excludedArbitrators, resultHandler, errorMessageHandler);
            }
        });
    }

    private void sendInitTradeRequest(NodeAddress arbitratorNodeAddress, SendDirectMessageListener listener) {

        // get registered arbitrator
        Arbitrator arbitrator = processModel.getUser().getAcceptedArbitratorByAddress(arbitratorNodeAddress);
        if (arbitrator == null) throw new RuntimeException("Node address " + arbitratorNodeAddress + " is not a registered arbitrator");

        // set pub keys
        processModel.getArbitrator().setPubKeyRing(arbitrator.getPubKeyRing());
        trade.getArbitrator().setNodeAddress(arbitratorNodeAddress);
        trade.getArbitrator().setPubKeyRing(processModel.getArbitrator().getPubKeyRing());

        // create request to arbitrator
        InitTradeRequest takerRequest = (InitTradeRequest) processModel.getTradeMessage(); // taker's InitTradeRequest to maker
        InitTradeRequest arbitratorRequest = new InitTradeRequest(
                takerRequest.getTradeProtocolVersion(),
                trade.getId(),
                trade.getAmount().longValueExact(),
                trade.getPrice().getValue(),
                trade.getOffer().getOfferPayload().getPaymentMethodId(),
                trade.getProcessModel().getAccountId(),
                takerRequest.getTakerAccountId(),
                trade.getOffer().getOfferPayload().getMakerPaymentAccountId(),
                takerRequest.getTakerPaymentAccountId(),
                trade.getTaker().getPubKeyRing(),
                takerRequest.getUid(),
                Version.getP2PMessageVersion(),
                null,
                takerRequest.getCurrentDate(),
                trade.getMaker().getNodeAddress(),
                trade.getTaker().getNodeAddress(),
                trade.getArbitrator().getNodeAddress(),
                trade.getSelf().getReserveTxHash(),
                trade.getSelf().getReserveTxHex(),
                trade.getSelf().getReserveTxKey(),
                model.getTskWalletService().getOrCreateAddressEntry(trade.getOffer().getId(), TskAddressEntry.Context.TRADE_PAYOUT).getAddressString());

        // send request to arbitrator
        log.info("Sending {} with offerId {} and uid {} to arbitrator {}", arbitratorRequest.getClass().getSimpleName(), arbitratorRequest.getOfferId(), arbitratorRequest.getUid(), trade.getArbitrator().getNodeAddress());
        processModel.getP2PService().sendEncryptedDirectMessage(
                arbitratorNodeAddress,
                arbitrator.getPubKeyRing(),
                arbitratorRequest,
                listener,
                TuskexUtils.ARBITRATOR_ACK_TIMEOUT_SECONDS
        );
    }
}
