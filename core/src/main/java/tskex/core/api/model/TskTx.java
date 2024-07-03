package tuskex.core.api.model;

import tuskex.common.Payload;
import tuskex.common.proto.ProtoUtil;
import lombok.Getter;
import monero.wallet.model.MoneroTxWallet;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static tuskex.core.api.model.TskIncomingTransfer.toTskIncomingTransfer;
import static tuskex.core.api.model.TskOutgoingTransfer.toTskOutgoingTransfer;

@Getter
public class TskTx implements Payload {

    private final String hash;
    private final BigInteger fee;
    private final boolean isConfirmed;
    private final boolean isLocked;
    @Nullable
    private final Long height;
    @Nullable
    private final Long timestamp;
    @Nullable
    private final List<TskIncomingTransfer> incomingTransfers;
    @Nullable
    private final TskOutgoingTransfer outgoingTransfer;
    @Nullable
    private final String metadata;

    public TskTx(TskTxBuilder builder) {
        this.hash = builder.hash;
        this.fee = builder.fee;
        this.isConfirmed = builder.isConfirmed;
        this.isLocked = builder.isLocked;
        this.height = builder.height;
        this.timestamp = builder.timestamp;
        this.incomingTransfers = builder.incomingTransfers;
        this.outgoingTransfer = builder.outgoingTransfer;
        this.metadata = builder.metadata;
    }

    public static TskTx toTskTx(MoneroTxWallet tx){
        Long timestamp = tx.getBlock() == null ? null : tx.getBlock().getTimestamp();
        List<TskIncomingTransfer> incomingTransfers = tx.getIncomingTransfers() == null ? null :
                tx.getIncomingTransfers().stream()
                .map(s -> toTskIncomingTransfer(s))
                .collect(Collectors.toList());
        TskOutgoingTransfer outgoingTransfer = tx.getOutgoingTransfer() == null ? null :
                toTskOutgoingTransfer(tx.getOutgoingTransfer());
        TskTxBuilder builder = new TskTxBuilder()
                .withHash(tx.getHash())
                .withFee(tx.getFee())
                .withIsConfirmed(tx.isConfirmed())
                .withIsLocked(tx.isLocked());
        Optional.ofNullable(tx.getHeight()).ifPresent(e ->builder.withHeight(tx.getHeight()));
        Optional.ofNullable(timestamp).ifPresent(e ->builder.withTimestamp(timestamp));
        Optional.ofNullable(outgoingTransfer).ifPresent(e ->builder.withOutgoingTransfer(outgoingTransfer));
        Optional.ofNullable(incomingTransfers).ifPresent(e ->builder.withIncomingTransfers(incomingTransfers));
        Optional.ofNullable(tx.getMetadata()).ifPresent(e ->builder.withMetadata(tx.getMetadata()));
        return builder.build();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public tuskex.proto.grpc.TskTx toProtoMessage() {
        tuskex.proto.grpc.TskTx.Builder builder = tuskex.proto.grpc.TskTx.newBuilder()
                .setHash(hash)
                .setFee(fee.toString())
                .setIsConfirmed(isConfirmed)
                .setIsLocked(isLocked);
        Optional.ofNullable(height).ifPresent(e -> builder.setHeight(height));
        Optional.ofNullable(timestamp).ifPresent(e -> builder.setTimestamp(timestamp));
        Optional.ofNullable(outgoingTransfer).ifPresent(e -> builder.setOutgoingTransfer(outgoingTransfer.toProtoMessage()));
        Optional.ofNullable(incomingTransfers).ifPresent(e -> builder.addAllIncomingTransfers(ProtoUtil.collectionToProto(incomingTransfers, tuskex.proto.grpc.TskIncomingTransfer.class)));
        Optional.ofNullable(metadata).ifPresent(e -> builder.setMetadata(metadata));
        return builder.build();
    }

    public static TskTx fromProto(tuskex.proto.grpc.TskTx proto) {
        return new TskTxBuilder()
                .withHash(proto.getHash())
                .withFee(new BigInteger(proto.getFee()))
                .withIsConfirmed(proto.getIsConfirmed())
                .withIsLocked(proto.getIsLocked())
                .withHeight(proto.getHeight())
                .withTimestamp(proto.getTimestamp())
                .withIncomingTransfers(
                    proto.getIncomingTransfersList().stream()
                        .map(TskIncomingTransfer::fromProto)
                        .collect(Collectors.toList()))
                .withOutgoingTransfer(TskOutgoingTransfer.fromProto(proto.getOutgoingTransfer()))
                .withMetadata(proto.getMetadata())
                .build();
    }

    public static class TskTxBuilder {
        private String hash;
        private BigInteger fee;
        private boolean isConfirmed;
        private boolean isLocked;
        private Long height;
        private Long timestamp;
        private List<TskIncomingTransfer> incomingTransfers;
        private TskOutgoingTransfer outgoingTransfer;
        private String metadata;

        public TskTxBuilder withHash(String hash) {
            this.hash = hash;
            return this;
        }

        public TskTxBuilder withFee(BigInteger fee) {
            this.fee = fee;
            return this;
        }

        public TskTxBuilder withIsConfirmed(boolean isConfirmed) {
            this.isConfirmed = isConfirmed;
            return this;
        }

        public TskTxBuilder withIsLocked(boolean isLocked) {
            this.isLocked = isLocked;
            return this;
        }

        public TskTxBuilder withHeight(Long height) {
            this.height = height;
            return this;
        }

        public TskTxBuilder withTimestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public TskTxBuilder withIncomingTransfers(List<TskIncomingTransfer> incomingTransfers) {
            this.incomingTransfers = incomingTransfers;
            return this;
        }

        public TskTxBuilder withOutgoingTransfer(TskOutgoingTransfer outgoingTransfer) {
            this.outgoingTransfer = outgoingTransfer;
            return this;
        }

        public TskTxBuilder withMetadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public TskTx build() { return new TskTx(this); }
    }

    @Override
    public String toString() {
        return "TskTx{" +
                "hash=" + hash +
                ", fee=" + timestamp +
                ", isConfirmed=" + isConfirmed +
                ", isLocked=" + isLocked +
                ", height=" + height +
                ", timestamp=" + timestamp +
                ", incomingTransfers=" + incomingTransfers +
                ", outgoingTransfer=" + outgoingTransfer +
                ", metadata=" + metadata +
                '}';
    }
}
