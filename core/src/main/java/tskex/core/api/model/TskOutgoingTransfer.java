package tuskex.core.api.model;

import tuskex.common.Payload;
import tuskex.common.proto.ProtoUtil;
import lombok.Getter;
import monero.wallet.model.MoneroOutgoingTransfer;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static tuskex.core.api.model.TskDestination.toTskDestination;

@Getter
public class TskOutgoingTransfer implements Payload {

    private final BigInteger amount;
    private final Integer accountIndex;
    @Nullable
    private final List<Integer> subaddressIndices;
    @Nullable
    private final List<TskDestination> destinations;

    public TskOutgoingTransfer(TskOutgoingTransferBuilder builder) {
        this.amount = builder.amount;
        this.accountIndex = builder.accountIndex;
        this.subaddressIndices = builder.subaddressIndices;
        this.destinations = builder.destinations;
    }

    public static TskOutgoingTransfer toTskOutgoingTransfer(MoneroOutgoingTransfer transfer) {
        List<TskDestination> destinations = transfer.getDestinations() == null ? null :
                transfer.getDestinations().stream()
                .map(s -> toTskDestination(s))
                .collect(Collectors.toList());
        TskOutgoingTransferBuilder builder = new TskOutgoingTransferBuilder()
                .withAmount(transfer.getAmount())
                .withAccountIndex(transfer.getAccountIndex())
                .withSubaddressIndices(transfer.getSubaddressIndices())
                .withDestinations(destinations);
        return builder.build();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public tuskex.proto.grpc.TskOutgoingTransfer toProtoMessage() {
        var builder = tuskex.proto.grpc.TskOutgoingTransfer.newBuilder()
                .setAmount(amount.toString())
                .setAccountIndex(accountIndex);
        Optional.ofNullable(subaddressIndices).ifPresent(e -> builder.addAllSubaddressIndices(subaddressIndices));
        Optional.ofNullable(destinations).ifPresent(e -> builder.addAllDestinations(ProtoUtil.collectionToProto(destinations, tuskex.proto.grpc.TskDestination.class)));
        return builder.build();
    }

    public static TskOutgoingTransfer fromProto(tuskex.proto.grpc.TskOutgoingTransfer proto) {
        List<TskDestination> destinations = proto.getDestinationsList().isEmpty() ?
                null : proto.getDestinationsList().stream()
                .map(TskDestination::fromProto).collect(Collectors.toList());
        return new TskOutgoingTransferBuilder()
                .withAmount(new BigInteger(proto.getAmount()))
                .withAccountIndex(proto.getAccountIndex())
                .withSubaddressIndices(proto.getSubaddressIndicesList())
                .withDestinations(destinations)
                .build();
    }

    public static class TskOutgoingTransferBuilder {
        private BigInteger amount;
        private Integer accountIndex;
        private List<Integer> subaddressIndices;
        private List<TskDestination> destinations;

        public TskOutgoingTransferBuilder withAmount(BigInteger amount) {
            this.amount = amount;
            return this;
        }

        public TskOutgoingTransferBuilder withAccountIndex(Integer accountIndex) {
            this.accountIndex = accountIndex;
            return this;
        }

        public TskOutgoingTransferBuilder withSubaddressIndices(List<Integer> subaddressIndices) {
            this.subaddressIndices = subaddressIndices;
            return this;
        }

        public TskOutgoingTransferBuilder withDestinations(List<TskDestination> destinations) {
            this.destinations = destinations;
            return this;
        }

        public TskOutgoingTransfer build() {
            return new TskOutgoingTransfer(this);
        }
    }

    @Override
    public String toString() {
        return "TskOutgoingTransfer{" +
                "amount=" + amount +
                ", accountIndex=" + accountIndex +
                ", subaddressIndices=" + subaddressIndices +
                ", destinations=" + destinations +
                '}';
    }
}
