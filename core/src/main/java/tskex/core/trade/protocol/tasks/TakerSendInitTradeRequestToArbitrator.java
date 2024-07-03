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
import tuskex.core.offer.Offer;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.InitTradeRequest;
import tuskex.core.trade.messages.TradeProtocolVersion;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.network.p2p.SendDirectMessageListener;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static tuskex.core.util.Validator.checkTradeId;

@Slf4j
public class TakerSendInitTradeRequestToArbitrator extends TradeTask {
    @SuppressWarnings({"unused"})
    public TakerSendInitTradeRequestToArbitrator(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // verify trade state
            InitTradeRequest sourceRequest = (InitTradeRequest) processModel.getTradeMessage(); // arbitrator's InitTradeRequest to taker
            checkNotNull(sourceRequest);
            checkTradeId(processModel.getOfferId(), sourceRequest);
            if (trade.getSelf().getReserveTxHash() == null || trade.getSelf().getReserveTxHash().isEmpty()) throw new IllegalStateException("Reserve tx id is not initialized: " + trade.getSelf().getReserveTxHash());

            // create request to arbitrator
            Offer offer = processModel.getOffer();
            InitTradeRequest arbitratorRequest = new InitTradeRequest(
                    TradeProtocolVersion.MULTISIG_2_3, // TODO: use processModel.getTradeProtocolVersion(), select one of maker's supported versions
                    offer.getId(),
                    trade.getAmount().longValueExact(),
                    trade.getPrice().getValue(),
                    trade.getSelf().getPaymentMethodId(),
                    trade.getMaker().getAccountId(),
                    trade.getTaker().getAccountId(),
                    trade.getMaker().getPaymentAccountId(),
                    trade.getTaker().getPaymentAccountId(),
                    trade.getTaker().getPubKeyRing(),
                    UUID.randomUUID().toString(),
                    Version.getP2PMessageVersion(),
                    null,
                    sourceRequest.getCurrentDate(),
                    trade.getMaker().getNodeAddress(),
                    trade.getTaker().getNodeAddress(),
                    trade.getArbitrator().getNodeAddress(),
                    trade.getSelf().getReserveTxHash(),
                    trade.getSelf().getReserveTxHex(),
                    trade.getSelf().getReserveTxKey(),
                    model.getTskWalletService().getAddressEntry(offer.getId(), TskAddressEntry.Context.TRADE_PAYOUT).get().getAddressString());

            // send request to arbitrator
            log.info("Sending {} with offerId {} and uid {} to arbitrator {}", arbitratorRequest.getClass().getSimpleName(), arbitratorRequest.getOfferId(), arbitratorRequest.getUid(), trade.getArbitrator().getNodeAddress());
            processModel.getP2PService().sendEncryptedDirectMessage(
                    trade.getArbitrator().getNodeAddress(),
                    trade.getArbitrator().getPubKeyRing(),
                    arbitratorRequest,
                    new SendDirectMessageListener() {
                        @Override
                        public void onArrived() {
                            log.info("{} arrived at arbitrator: offerId={}", InitTradeRequest.class.getSimpleName(), trade.getId());
                            complete();
                        }
                        @Override
                        public void onFault(String errorMessage) {
                            log.warn("Failed to send {} to arbitrator, error={}.", InitTradeRequest.class.getSimpleName(), errorMessage);
                            failed();
                        }
                    });
        } catch (Throwable t) {
            failed(t);
        }
    }
}
