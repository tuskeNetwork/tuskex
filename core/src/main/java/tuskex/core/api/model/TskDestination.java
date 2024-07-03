package tuskex.core.api.model;

import tuskex.common.Payload;
import lombok.Getter;
import monero.wallet.model.MoneroDestination;

import java.math.BigInteger;

@Getter
public class TskDestination implements Payload {

    private final String address;
    private final BigInteger amount;

    public TskDestination(TskDestinationBuilder builder) {
        this.address = builder.address;
        this.amount = builder.amount;
    }

    public static TskDestination toTskDestination(MoneroDestination dst) {
        return new TskDestinationBuilder()
                .withAddress(dst.getAddress())
                .withAmount(dst.getAmount())
                .build();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public tuskex.proto.grpc.TskDestination toProtoMessage() {
        return tuskex.proto.grpc.TskDestination.newBuilder()
                .setAddress(address)
                .setAmount(amount.toString())
                .build();
    }

    public static TskDestination fromProto(tuskex.proto.grpc.TskDestination proto) {
        return new TskDestinationBuilder()
                .withAddress(proto.getAddress())
                .withAmount(new BigInteger(proto.getAmount()))
                .build();
    }

    public static class TskDestinationBuilder {
        private String address;
        private BigInteger amount;

        public TskDestinationBuilder withAddress(String address) {
            this.address = address;
            return this;
        }

        public TskDestinationBuilder withAmount(BigInteger amount) {
            this.amount = amount;
            return this;
        }

        public TskDestination build() { return new TskDestination(this); }
    }

    @Override
    public String toString() {
        return "TskDestination{" +
                "address=" + address +
                ", amount" + amount +
                '}';
    }
}
