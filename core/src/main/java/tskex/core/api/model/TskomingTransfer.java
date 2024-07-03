package tuskex.core.api.model;

import tuskex.common.Payload;
import lombok.Getter;
import monero.wallet.model.MoneroIncomingTransfer;

import java.math.BigInteger;

@Getter
public class TskIncomingTransfer implements Payload {

    private final BigInteger amount;
    private final Integer accountIndex;
    private final Integer subaddressIndex;
    private final String address;
    private final Long numSuggestedConfirmations;

    public TskIncomingTransfer(TskIncomingTransferBuilder builder) {
        this.amount = builder.amount;
        this.accountIndex = builder.accountIndex;
        this.subaddressIndex = builder.subaddressIndex;
        this.address = builder.address;
        this.numSuggestedConfirmations = builder.numSuggestedConfirmations;
    }

    public static TskIncomingTransfer toTskIncomingTransfer(MoneroIncomingTransfer transfer) {
        return new TskIncomingTransferBuilder()
                .withAmount(transfer.getAmount())
                .withAccountIndex(transfer.getAccountIndex())
                .withSubaddressIndex(transfer.getSubaddressIndex())
                .withAddress(transfer.getAddress())
                .withNumSuggestedConfirmations(transfer.getNumSuggestedConfirmations())
                .build();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public tuskex.proto.grpc.TskIncomingTransfer toProtoMessage() {
        return tuskex.proto.grpc.TskIncomingTransfer.newBuilder()
                .setAmount(amount.toString())
                .setAccountIndex(accountIndex)
                .setSubaddressIndex(subaddressIndex)
                .setAddress(address)
                .setNumSuggestedConfirmations(numSuggestedConfirmations)
                .build();
    }

    public static TskIncomingTransfer fromProto(tuskex.proto.grpc.TskIncomingTransfer proto) {
        return new TskIncomingTransferBuilder()
                .withAmount(new BigInteger(proto.getAmount()))
                .withAccountIndex(proto.getAccountIndex())
                .withSubaddressIndex(proto.getSubaddressIndex())
                .withAddress(proto.getAddress())
                .withNumSuggestedConfirmations(proto.getNumSuggestedConfirmations())
                .build();
    }

    public static class TskIncomingTransferBuilder {
        private BigInteger amount;
        private Integer accountIndex;
        private Integer subaddressIndex;
        private String address;
        private Long numSuggestedConfirmations;

        public TskIncomingTransferBuilder withAmount(BigInteger amount) {
            this.amount = amount;
            return this;
        }

        public TskIncomingTransferBuilder withAccountIndex(Integer accountIndex) {
            this.accountIndex = accountIndex;
            return this;
        }

        public TskIncomingTransferBuilder withSubaddressIndex(Integer subaddressIndex) {
            this.subaddressIndex = subaddressIndex;
            return this;
        }

        public TskIncomingTransferBuilder withAddress(String address) {
            this.address = address;
            return this;
        }

        public TskIncomingTransferBuilder withNumSuggestedConfirmations(Long numSuggestedConfirmations) {
            this.numSuggestedConfirmations = numSuggestedConfirmations;
            return this;
        }

        public TskIncomingTransfer build() {
            return new TskIncomingTransfer(this);
        }
    }

    @Override
    public String toString() {
        return "TskIncomingTransfer{" +
                "amount=" + amount +
                ", accountIndex=" + accountIndex +
                ", subaddressIndex=" + subaddressIndex +
                ", address=" + address +
                ", numSuggestedConfirmations=" + numSuggestedConfirmations +
                '}';
    }
}
