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
import tuskex.core.trade.TuskexUtils;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.InitTradeRequest;
import tuskex.core.trade.messages.TradeProtocolVersion;
import tuskex.core.user.User;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.network.p2p.P2PService;
import tuskex.network.p2p.SendDirectMessageListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.UUID;

@Slf4j
public class TakerSendInitTradeRequestToMaker extends TradeTask {
    @SuppressWarnings({"unused"})
    public TakerSendInitTradeRequestToMaker(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // verify trade state
            if (trade.getSelf().getReserveTxHash() == null || trade.getSelf().getReserveTxHash().isEmpty()) throw new IllegalStateException("Reserve tx id is not initialized: " + trade.getSelf().getReserveTxHash());

            // collect fields
            Offer offer = model.getOffer();
            User user = processModel.getUser();
            P2PService p2PService = processModel.getP2PService();
            TskWalletService walletService = model.getTskWalletService();
            String payoutAddress = walletService.getOrCreateAddressEntry(offer.getId(), TskAddressEntry.Context.TRADE_PAYOUT).getAddressString();

            // taker signs offer using offer id as nonce to avoid challenge protocol
            byte[] sig = TuskexUtils.sign(p2PService.getKeyRing(), offer.getId());

            // create request to maker
            InitTradeRequest makerRequest = new InitTradeRequest(
                    TradeProtocolVersion.MULTISIG_2_3, // TODO: use processModel.getTradeProtocolVersion(), select one of maker's supported versions
                    offer.getId(),
                    trade.getAmount().longValueExact(),
                    trade.getPrice().getValue(),
                    trade.getSelf().getPaymentMethodId(),
                    null,
                    user.getAccountId(),
                    trade.getMaker().getPaymentAccountId(),
                    trade.getTaker().getPaymentAccountId(),
                    p2PService.getKeyRing().getPubKeyRing(),
                    UUID.randomUUID().toString(),
                    Version.getP2PMessageVersion(),
                    sig,
                    new Date().getTime(),
                    offer.getMakerNodeAddress(),
                    P2PService.getMyNodeAddress(),
                    null, // maker selects arbitrator
                    null, // reserve tx not sent from taker to maker
                    null,
                    null,
                    payoutAddress);

            // send request to maker
            log.info("Sending {} with offerId {} and uid {} to maker {}", makerRequest.getClass().getSimpleName(), makerRequest.getOfferId(), makerRequest.getUid(), trade.getMaker().getNodeAddress());
            processModel.getP2PService().sendEncryptedDirectMessage(
                    trade.getMaker().getNodeAddress(),
                    trade.getMaker().getPubKeyRing(),
                    makerRequest,
                    new SendDirectMessageListener() {
                        @Override
                        public void onArrived() {
                            log.info("{} arrived at maker: offerId={}", InitTradeRequest.class.getSimpleName(), trade.getId());
                            complete();
                        }
                        @Override
                        public void onFault(String errorMessage) {
                            log.warn("Failed to send {} to maker, error={}.", InitTradeRequest.class.getSimpleName(), errorMessage);
                            failed();
                        }
                    });
        } catch (Throwable t) {
            failed(t);
        }
    }
}
