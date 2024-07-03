package tuskex.apitest.method.wallet;

import tuskex.proto.grpc.BtcBalanceInfo;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class WalletTestUtil {

    // All api tests depend on the DAO / regtest environment, and Bob & Alice's wallets
    // are initialized with 10 BTC during the scaffolding setup.
    public static final tuskex.core.api.model.BtcBalanceInfo INITIAL_BTC_BALANCES =
            tuskex.core.api.model.BtcBalanceInfo.valueOf(1000000000,
                    0,
                    1000000000,
                    0);

    public static void verifyBtcBalances(tuskex.core.api.model.BtcBalanceInfo expected,
                                         BtcBalanceInfo actual) {
        assertEquals(expected.getAvailableBalance(), actual.getAvailableBalance());
        assertEquals(expected.getReservedBalance(), actual.getReservedBalance());
        assertEquals(expected.getTotalAvailableBalance(), actual.getTotalAvailableBalance());
        assertEquals(expected.getLockedBalance(), actual.getLockedBalance());
    }
}
