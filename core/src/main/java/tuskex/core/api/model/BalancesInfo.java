package tuskex.core.api.model;

import tuskex.common.Payload;
import lombok.Getter;

@Getter
public class BalancesInfo implements Payload {

    // Getter names are shortened for readability's sake, i.e.,
    // balancesInfo.getBtc().getAvailableBalance() is cleaner than
    // balancesInfo.getBtcBalanceInfo().getAvailableBalance().
    private final BtcBalanceInfo btc;
    private final TskBalanceInfo tsk;

    public BalancesInfo(BtcBalanceInfo btc, TskBalanceInfo tsk) {
        this.btc = btc;
        this.tsk = tsk;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public tuskex.proto.grpc.BalancesInfo toProtoMessage() {
        return tuskex.proto.grpc.BalancesInfo.newBuilder()
                .setBtc(btc.toProtoMessage())
                .setTsk(tsk.toProtoMessage())
                .build();
    }

    public static BalancesInfo fromProto(tuskex.proto.grpc.BalancesInfo proto) {
        return new BalancesInfo(
                BtcBalanceInfo.fromProto(proto.getBtc()),
                TskBalanceInfo.fromProto(proto.getTsk()));
    }

    @Override
    public String toString() {
        return "BalancesInfo{" + "\n" +
                " " + btc.toString() + "\n" +
                ", " + tsk.toString() + "\n" +
                '}';
    }
}
