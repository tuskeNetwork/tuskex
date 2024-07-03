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

package tuskex.core.offer.availability.tasks;

import tuskex.common.app.Version;
import tuskex.common.taskrunner.Task;
import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.monetary.Price;
import tuskex.core.offer.Offer;
import tuskex.core.offer.availability.OfferAvailabilityModel;
import tuskex.core.offer.messages.OfferAvailabilityRequest;
import tuskex.core.trade.TuskexUtils;
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

// TODO (woodser): rename to TakerSendOfferAvailabilityRequest and group with other taker tasks
@Slf4j
public class SendOfferAvailabilityRequest extends Task<OfferAvailabilityModel> {
    public SendOfferAvailabilityRequest(TaskRunner<OfferAvailabilityModel> taskHandler, OfferAvailabilityModel model) {
        super(taskHandler, model);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // collect fields
            Offer offer = model.getOffer();
            User user = model.getUser();
            P2PService p2PService = model.getP2PService();
            TskWalletService walletService = model.getTskWalletService();
            String makerPaymentAccountId = offer.getOfferPayload().getMakerPaymentAccountId();
            String takerPaymentAccountId = model.getPaymentAccountId();
            String paymentMethodId = user.getPaymentAccount(takerPaymentAccountId).getPaymentAccountPayload().getPaymentMethodId();
            String payoutAddress = walletService.getOrCreateAddressEntry(offer.getId(), TskAddressEntry.Context.TRADE_PAYOUT).getAddressString();

            // taker signs offer using offer id as nonce to avoid challenge protocol
            byte[] sig = TuskexUtils.sign(model.getP2PService().getKeyRing(), offer.getId());

            // get price
            Price price = offer.getPrice();
            if (price == null) throw new RuntimeException("Could not get price for offer");

            // send InitTradeRequest to maker to sign
            InitTradeRequest tradeRequest = new InitTradeRequest(
                    TradeProtocolVersion.MULTISIG_2_3, // TODO: replace with first of their accepted protocols
                    offer.getId(),
                    model.getTradeAmount().longValueExact(),
                    price.getValue(),
                    paymentMethodId,
                    null,
                    user.getAccountId(),
                    makerPaymentAccountId,
                    takerPaymentAccountId,
                    p2PService.getKeyRing().getPubKeyRing(),
                    UUID.randomUUID().toString(),
                    Version.getP2PMessageVersion(),
                    sig,
                    new Date().getTime(),
                    offer.getMakerNodeAddress(),
                    P2PService.getMyNodeAddress(),
                    null, // maker provides node address of backup arbitrator on response
                    null, // reserve tx not sent from taker to maker
                    null,
                    null,
                    payoutAddress);

            // save trade request to later send to arbitrator
            model.setTradeRequest(tradeRequest);

            OfferAvailabilityRequest message = new OfferAvailabilityRequest(model.getOffer().getId(),
                    model.getPubKeyRing(), model.getTakersTradePrice(), model.isTakerApiUser(), tradeRequest);
            log.info("Send {} with offerId {} and uid {} to peer {}",
                    message.getClass().getSimpleName(), message.getOfferId(),
                    message.getUid(), model.getPeerNodeAddress());

            model.getP2PService().sendEncryptedDirectMessage(model.getPeerNodeAddress(),
                    model.getOffer().getPubKeyRing(),
                    message,
                    new SendDirectMessageListener() {
                        @Override
                        public void onArrived() {
                            log.info("{} arrived at peer: offerId={}; uid={}",
                                    message.getClass().getSimpleName(), message.getOfferId(), message.getUid());
                            complete();
                        }

                        @Override
                        public void onFault(String errorMessage) {
                            log.error("Sending {} failed: uid={}; peer={}; error={}",
                                    message.getClass().getSimpleName(), message.getUid(),
                                    model.getPeerNodeAddress(), errorMessage);
                            model.getOffer().setState(Offer.State.MAKER_OFFLINE);
                        }
                    }
            );
        } catch (Throwable t) {
            model.getOffer().setErrorMessage("An error occurred.\n" +
                    "Error message:\n"
                    + t.getMessage());

            failed(t);
        }
    }
}

