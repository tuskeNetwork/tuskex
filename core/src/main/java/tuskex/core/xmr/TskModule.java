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

package tuskex.core.tsk;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import tuskex.common.app.AppModule;
import tuskex.common.config.Config;
import tuskex.core.provider.ProvidersRepository;
import tuskex.core.provider.fee.FeeProvider;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.tsk.model.AddressEntryList;
import tuskex.core.tsk.model.EncryptedConnectionList;
import tuskex.core.tsk.model.TskAddressEntryList;
import tuskex.core.tsk.nodes.TskNodes;
import tuskex.core.tsk.setup.RegTestHost;
import tuskex.core.tsk.setup.WalletsSetup;
import tuskex.core.tsk.wallet.BtcWalletService;
import tuskex.core.tsk.wallet.NonBsqCoinSelector;
import tuskex.core.tsk.wallet.TradeWalletService;
import tuskex.core.tsk.wallet.TskWalletService;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.google.inject.name.Names.named;
import static tuskex.common.config.Config.PROVIDERS;
import static tuskex.common.config.Config.WALLET_DIR;
import static tuskex.common.config.Config.WALLET_RPC_BIND_PORT;

public class TskModule extends AppModule {

    public TskModule(Config config) {
        super(config);
    }

    @Override
    protected void configure() {
        // If we have selected BTC_DAO_REGTEST or BTC_DAO_TESTNET we use our master regtest node,
        // otherwise the specified host or default (localhost)
        String regTestHost = config.bitcoinRegtestHost;
        if (regTestHost.isEmpty()) {
            regTestHost = Config.DEFAULT_REGTEST_HOST;
        }

        RegTestHost.HOST = regTestHost;
        if (Arrays.asList("localhost", "127.0.0.1").contains(regTestHost)) {
            bind(RegTestHost.class).toInstance(RegTestHost.LOCALHOST);
        } else if ("none".equals(regTestHost)) {
            bind(RegTestHost.class).toInstance(RegTestHost.NONE);
        } else {
            bind(RegTestHost.class).toInstance(RegTestHost.REMOTE_HOST);
        }

        bind(File.class).annotatedWith(named(WALLET_DIR)).toInstance(config.walletDir);
        bind(int.class).annotatedWith(named(WALLET_RPC_BIND_PORT)).toInstance(config.walletRpcBindPort);

        bindConstant().annotatedWith(named(Config.TSK_NODE)).to(config.tskNode);
        bindConstant().annotatedWith(named(Config.TSK_NODE_USERNAME)).to(config.tskNodeUsername);
        bindConstant().annotatedWith(named(Config.TSK_NODE_PASSWORD)).to(config.tskNodePassword);
        bindConstant().annotatedWith(named(Config.TSK_NODES)).to(config.tskNodes);
        bindConstant().annotatedWith(named(Config.USE_NATIVE_TSK_WALLET)).to(config.useNativeTskWallet);
        bindConstant().annotatedWith(named(Config.USER_AGENT)).to(config.userAgent);
        bindConstant().annotatedWith(named(Config.NUM_CONNECTIONS_FOR_BTC)).to(config.numConnectionsForBtc);
        bindConstant().annotatedWith(named(Config.USE_ALL_PROVIDED_NODES)).to(config.useAllProvidedNodes);
        bindConstant().annotatedWith(named(Config.SOCKS5_DISCOVER_MODE)).to(config.socks5DiscoverMode);
        bind(new TypeLiteral<List<String>>(){}).annotatedWith(named(PROVIDERS)).toInstance(config.providers);

        bind(AddressEntryList.class).in(Singleton.class);
        bind(TskAddressEntryList.class).in(Singleton.class);
        bind(EncryptedConnectionList.class).in(Singleton.class);
        bind(WalletsSetup.class).in(Singleton.class);
        bind(TskWalletService.class).in(Singleton.class);
        bind(BtcWalletService.class).in(Singleton.class);
        bind(TradeWalletService.class).in(Singleton.class);
        bind(NonBsqCoinSelector.class).in(Singleton.class);
        bind(TskNodes.class).in(Singleton.class);
        bind(Balances.class).in(Singleton.class);

        bind(ProvidersRepository.class).in(Singleton.class);
        bind(FeeProvider.class).in(Singleton.class);
        bind(PriceFeedService.class).in(Singleton.class);
    }
}

