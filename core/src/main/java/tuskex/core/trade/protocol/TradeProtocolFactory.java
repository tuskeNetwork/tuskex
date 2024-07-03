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

package tuskex.core.trade.protocol;

import tuskex.core.trade.ArbitratorTrade;
import tuskex.core.trade.BuyerAsMakerTrade;
import tuskex.core.trade.BuyerAsTakerTrade;
import tuskex.core.trade.SellerAsMakerTrade;
import tuskex.core.trade.SellerAsTakerTrade;
import tuskex.core.trade.Trade;

public class TradeProtocolFactory {
    public static TradeProtocol getNewTradeProtocol(Trade trade) {
        if (trade instanceof BuyerAsMakerTrade) {
            return new BuyerAsMakerProtocol((BuyerAsMakerTrade) trade);
        } else if (trade instanceof BuyerAsTakerTrade) {
            return new BuyerAsTakerProtocol((BuyerAsTakerTrade) trade);
        } else if (trade instanceof SellerAsMakerTrade) {
            return new SellerAsMakerProtocol((SellerAsMakerTrade) trade);
        } else if (trade instanceof SellerAsTakerTrade) {
            return new SellerAsTakerProtocol((SellerAsTakerTrade) trade);
        } else if (trade instanceof ArbitratorTrade) {
            return new ArbitratorProtocol((ArbitratorTrade) trade);
        } else {
            throw new IllegalStateException("Trade not of expected type. Trade=" + trade);
        }
    }
}
