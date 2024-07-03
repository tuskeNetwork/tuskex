package tuskex.core.api.model;

import java.math.BigInteger;

import com.google.common.annotations.VisibleForTesting;
import tuskex.common.Payload;

public class TskBalanceInfo implements Payload {

    public static final TskBalanceInfo EMPTY = new TskBalanceInfo(-1,
            -1,
            -1,
            -1,
            -1);

    // all balances are in atomic units
    private final long balance;
    private final long availableBalance;
    private final long pendingBalance;
    private final long reservedOfferBalance;
    private final long reservedTradeBalance;
    private final long reservedBalance;

    public TskBalanceInfo(long balance,
                          long unlockedBalance,
                          long pendingBalance,
                          long reservedOfferBalance,
                          long reservedTradeBalance) {
        this.balance = balance;
        this.availableBalance = unlockedBalance;
        this.pendingBalance = pendingBalance;
        this.reservedOfferBalance = reservedOfferBalance;
        this.reservedTradeBalance = reservedTradeBalance;
        this.reservedBalance = reservedOfferBalance + reservedTradeBalance;
    }

    @VisibleForTesting
    public static TskBalanceInfo valueOf(long balance,
                                         long availableBalance,
                                         long pendingBalance,
                                         long reservedOfferBalance,
                                         long reservedTradeBalance) {
        return new TskBalanceInfo(balance,
                availableBalance,
                pendingBalance,
                reservedOfferBalance,
                reservedTradeBalance);
    }

    public BigInteger getBalance() {
        return BigInteger.valueOf(balance);
    }

    public BigInteger getAvailableBalance() {
        return BigInteger.valueOf(availableBalance);
    }

    public BigInteger getPendingBalance() {
        return BigInteger.valueOf(pendingBalance);
    }

    public BigInteger getReservedOfferBalance() {
        return BigInteger.valueOf(reservedOfferBalance);
    }

    public BigInteger getReservedTradeBalance() {
        return BigInteger.valueOf(reservedTradeBalance);
    }

    public BigInteger getReservedBalance() {
        return BigInteger.valueOf(reservedBalance);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public tuskex.proto.grpc.TskBalanceInfo toProtoMessage() {
        return tuskex.proto.grpc.TskBalanceInfo.newBuilder()
                .setBalance(balance)
                .setAvailableBalance(availableBalance)
                .setPendingBalance(pendingBalance)
                .setReservedOfferBalance(reservedOfferBalance)
                .setReservedTradeBalance(reservedTradeBalance)
                .build();
    }

    public static TskBalanceInfo fromProto(tuskex.proto.grpc.TskBalanceInfo proto) {
        return new TskBalanceInfo(proto.getBalance(),
                proto.getAvailableBalance(),
                proto.getPendingBalance(),
                proto.getReservedOfferBalance(),
                proto.getReservedTradeBalance());
    }

    @Override
    public String toString() {
        return "TskBalanceInfo{" +
                "balance=" + balance +
                "unlockedBalance=" + availableBalance +
                ", lockedBalance=" + pendingBalance +
                ", reservedOfferBalance=" + reservedOfferBalance +
                ", reservedTradeBalance=" + reservedTradeBalance +
                '}';
    }
}
