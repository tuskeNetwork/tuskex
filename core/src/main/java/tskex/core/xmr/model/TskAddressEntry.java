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

package tuskex.core.tsk.model;

import tuskex.common.proto.ProtoUtil;
import tuskex.common.proto.persistable.PersistablePayload;
import tuskex.common.util.Utilities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Coin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Every trade uses a TskAddressEntry with a dedicated address for all transactions related to the trade.
 * That way we have a kind of separated trade wallet, isolated from other transactions and avoiding coin merge.
 * If we would not avoid coin merge the user would lose privacy between trades.
 */
@EqualsAndHashCode
@Slf4j
public final class TskAddressEntry implements PersistablePayload {

    public enum Context {
        ARBITRATOR,
        BASE_ADDRESS,
        AVAILABLE,
        OFFER_FUNDING,
        TRADE_PAYOUT;
    }

    // keyPair can be null in case the object is created from deserialization as it is transient.
    // It will be restored when the wallet is ready at setDeterministicKey
    // So after startup it must never be null

    @Nullable
    @Getter
    private final String offerId;
    @Getter
    private final Context context;
    @Getter
    private final int subaddressIndex;
    @Getter
    private final String addressString;

    private long coinLockedInMultiSig;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, initialization
    ///////////////////////////////////////////////////////////////////////////////////////////

    public TskAddressEntry(int subaddressIndex, String address, Context context) {
      this(subaddressIndex, address, context, null, null);
    }

    public TskAddressEntry(int subaddressIndex, String address, Context context, @Nullable String offerId, Coin coinLockedInMultiSig) {
      this.subaddressIndex = subaddressIndex;
      this.addressString = address;
      this.offerId = offerId;
      this.context = context;
      if (coinLockedInMultiSig != null) this.coinLockedInMultiSig = coinLockedInMultiSig.value;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    public static TskAddressEntry fromProto(protobuf.TskAddressEntry proto) {
        return new TskAddressEntry(proto.getSubaddressIndex(),
                ProtoUtil.stringOrNullFromProto(proto.getAddressString()),
                ProtoUtil.enumFromProto(TskAddressEntry.Context.class, proto.getContext().name()),
                ProtoUtil.stringOrNullFromProto(proto.getOfferId()),
                Coin.valueOf(proto.getCoinLockedInMultiSig()));
    }

    @Override
    public protobuf.TskAddressEntry toProtoMessage() {
        protobuf.TskAddressEntry.Builder builder = protobuf.TskAddressEntry.newBuilder()
                .setSubaddressIndex(subaddressIndex)
                .setAddressString(addressString)
                .setContext(protobuf.TskAddressEntry.Context.valueOf(context.name()))
                .setCoinLockedInMultiSig(coinLockedInMultiSig);
        Optional.ofNullable(offerId).ifPresent(builder::setOfferId);
        return builder.build();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void setCoinLockedInMultiSig(@NotNull Coin coinLockedInMultiSig) {
        this.coinLockedInMultiSig = coinLockedInMultiSig.value;
    }

    // For display we usually only display the first 8 characters.
    @Nullable
    public String getShortOfferId() {
        return offerId != null ? Utilities.getShortId(offerId) : null;
    }

    public boolean isOpenOffer() {
        return context == Context.OFFER_FUNDING;
    }

    public boolean isTrade() {
        return context == Context.TRADE_PAYOUT;
    }

    public boolean isTradable() {
        return isOpenOffer() || isTrade();
    }

    public Coin getCoinLockedInMultiSig() {
        return Coin.valueOf(coinLockedInMultiSig);
    }

    @Override
    public String toString() {
        return "TskAddressEntry{" +
                "offerId='" + getOfferId() + '\'' +
                ", context=" + context +
                ", subaddressIndex=" + getSubaddressIndex() +
                ", address=" + getAddressString() +
                '}';
    }
}
