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

import tuskex.common.crypto.PubKeyRing;
import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.offer.Offer;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.trade.ArbitratorTrade;
import tuskex.core.trade.Trade;
import tuskex.core.trade.protocol.TradePeer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class VerifyPeersAccountAgeWitness extends TradeTask {

    public VerifyPeersAccountAgeWitness(TaskRunner<Trade> taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // only verify traditional offer
            Offer offer = checkNotNull(trade.getOffer());
            if (CurrencyUtil.isCryptoCurrency(offer.getCurrencyCode())) {
                complete();
                return;
            }

            // skip if arbitrator
            if (trade instanceof ArbitratorTrade) {
                complete();
                return;
            }

            // skip if payment account payload is null
            TradePeer tradePeer = trade.getTradePeer();
            if (tradePeer.getPaymentAccountPayload() == null) {
                complete();
                return;
            }

            AccountAgeWitnessService accountAgeWitnessService = processModel.getAccountAgeWitnessService();
            PaymentAccountPayload peersPaymentAccountPayload = checkNotNull(tradePeer.getPaymentAccountPayload(),
                    "Peers peersPaymentAccountPayload must not be null");
            PubKeyRing peersPubKeyRing = checkNotNull(tradePeer.getPubKeyRing(), "peersPubKeyRing must not be null");
            byte[] nonce = checkNotNull(tradePeer.getAccountAgeWitnessNonce());
            byte[] signature = checkNotNull(tradePeer.getAccountAgeWitnessSignature());
            AtomicReference<String> errorMsg = new AtomicReference<>();
            boolean isValid = accountAgeWitnessService.verifyAccountAgeWitness(trade,
                    peersPaymentAccountPayload,
                    peersPubKeyRing,
                    nonce,
                    signature,
                    errorMsg::set);
            if (isValid) {
                trade.getTradePeer().setAccountAgeWitness(processModel.getAccountAgeWitnessService().findWitness(trade.getTradePeer().getPaymentAccountPayload(), trade.getTradePeer().getPubKeyRing()).orElse(null));
                log.info("{} {} verified witness data of peer {}", trade.getClass().getSimpleName(), trade.getId(), tradePeer.getNodeAddress());
                complete();
            } else {
                failed(errorMsg.get());
            }
        } catch (Throwable t) {
            failed(t);
        }
    }
}
