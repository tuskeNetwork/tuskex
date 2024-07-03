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

import tuskex.common.proto.ProtoUtil;
import tuskex.core.offer.Offer;
import tuskex.core.proto.CoreProtoResolver;
import tuskex.core.trade.protocol.ProcessModel;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.network.p2p.NodeAddress;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.UUID;

@Slf4j
public final class SellerAsMakerTrade extends SellerTrade implements MakerTrade {

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, initialization
    ///////////////////////////////////////////////////////////////////////////////////////////

    public SellerAsMakerTrade(Offer offer,
                              BigInteger tradeAmount,
                              long tradePrice,
                              TskWalletService tskWalletService,
                              ProcessModel processModel,
                              String uid,
                              @Nullable NodeAddress makerNodeAddress,
                              @Nullable NodeAddress takerNodeAddress,
                              @Nullable NodeAddress arbitratorNodeAddress) {
        super(offer,
                tradeAmount,
                tradePrice,
                tskWalletService,
                processModel,
                uid,
                makerNodeAddress,
                takerNodeAddress,
                arbitratorNodeAddress);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public protobuf.Tradable toProtoMessage() {
        return protobuf.Tradable.newBuilder()
                .setSellerAsMakerTrade(protobuf.SellerAsMakerTrade.newBuilder()
                        .setTrade((protobuf.Trade) super.toProtoMessage()))
                .build();
    }

    public static Tradable fromProto(protobuf.SellerAsMakerTrade sellerAsMakerTradeProto,
                                     TskWalletService tskWalletService,
                                     CoreProtoResolver coreProtoResolver) {
        protobuf.Trade proto = sellerAsMakerTradeProto.getTrade();
        ProcessModel processModel = ProcessModel.fromProto(proto.getProcessModel(), coreProtoResolver);
        String uid = ProtoUtil.stringOrNullFromProto(proto.getUid());
        if (uid == null) {
            uid = UUID.randomUUID().toString();
        }
        SellerAsMakerTrade trade = new SellerAsMakerTrade(
                Offer.fromProto(proto.getOffer()),
                BigInteger.valueOf(proto.getAmount()),
                proto.getPrice(),
                tskWalletService,
                processModel,
                uid,
                proto.getProcessModel().getMaker().hasNodeAddress() ? NodeAddress.fromProto(proto.getProcessModel().getMaker().getNodeAddress()) : null,
                proto.getProcessModel().getTaker().hasNodeAddress() ? NodeAddress.fromProto(proto.getProcessModel().getTaker().getNodeAddress()) : null,
                proto.getProcessModel().getArbitrator().hasNodeAddress() ? NodeAddress.fromProto(proto.getProcessModel().getArbitrator().getNodeAddress()) : null);

        trade.setPrice(proto.getPrice());

        return fromProto(trade,
                proto,
                coreProtoResolver);
    }
}
