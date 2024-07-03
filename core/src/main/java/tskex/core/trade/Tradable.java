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

package tuskex.core.trade;

import tuskex.common.proto.persistable.PersistablePayload;
import tuskex.core.monetary.Price;
import tuskex.core.monetary.Volume;
import tuskex.core.offer.Offer;
import tuskex.network.p2p.NodeAddress;

import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;

public interface Tradable extends PersistablePayload {
    Offer getOffer();

    Date getDate();

    String getId();

    String getShortId();

    default Optional<Trade> asTradeModel() {
        if (this instanceof Trade) {
            return Optional.of(((Trade) this));
        } else {
            return Optional.empty();
        }
    }

    default Optional<Volume> getOptionalVolume() {
        return asTradeModel().map(Trade::getVolume).or(() -> Optional.ofNullable(getOffer().getVolume()));
    }

    default Optional<Price> getOptionalPrice() {
        return asTradeModel().map(Trade::getPrice).or(() -> Optional.ofNullable(getOffer().getPrice()));
    }

    default Optional<BigInteger> getOptionalAmount() {
        return asTradeModel().map(Trade::getAmount);
    }

    default BigInteger getTotalTxFee() {
        return asTradeModel().map(Trade::getTotalTxFee).orElse(BigInteger.ZERO);
    }

    default Optional<BigInteger> getOptionalTakerFee() {
        return asTradeModel().map(Trade::getTakerFee);
    }

    default Optional<BigInteger> getOptionalMakerFee() {
        return asTradeModel().map(Trade::getMakerFee);
    }

    default Optional<NodeAddress> getOptionalTradePeerNodeAddress() {
        return asTradeModel().map(Trade::getTradePeerNodeAddress);
    }
}
