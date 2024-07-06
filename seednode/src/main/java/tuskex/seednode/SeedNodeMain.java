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

package tuskex.seednode;

import com.google.inject.Key;
import com.google.inject.name.Names;
import tuskex.common.Timer;
import tuskex.common.UserThread;
import tuskex.common.app.AppModule;
import tuskex.common.app.Capabilities;
import tuskex.common.app.Capability;
import tuskex.common.config.BaseCurrencyNetwork;
import tuskex.common.config.Config;
import tuskex.common.handlers.ResultHandler;
import tuskex.core.app.TorSetup;
import tuskex.core.app.misc.ExecutableForAppWithP2p;
import tuskex.core.app.misc.ModuleForAppWithP2p;
import tuskex.core.user.Cookie;
import tuskex.core.user.CookieKey;
import tuskex.core.user.User;
import tuskex.network.p2p.P2PService;
import tuskex.network.p2p.P2PServiceListener;
import tuskex.network.p2p.peers.PeerManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SeedNodeMain extends ExecutableForAppWithP2p {
    private static final long CHECK_CONNECTION_LOSS_SEC = 30;
    private static final String VERSION = "0.1.0";
    private SeedNode seedNode;
    private Timer checkConnectionLossTime;

    public SeedNodeMain() {
        super("Tuskex Seednode", "tuskex-seednode", "tuskex_seednode", VERSION);
    }

    public static void main(String[] args) {
        System.out.println("SeedNode.VERSION: " + VERSION);
        new SeedNodeMain().execute(args);
    }

    @Override
    protected int doExecute() {
        super.doExecute();

        checkMemory(config, this);

        return keepRunning();
    }

    @Override
    protected void addCapabilities() {
        Capabilities.app.addAll(Capability.SEED_NODE);
    }

    @Override
    protected void launchApplication() {
        UserThread.execute(() -> {
            try {
                seedNode = new SeedNode();
                UserThread.execute(this::onApplicationLaunched);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onApplicationLaunched() {
        super.onApplicationLaunched();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // We continue with a series of synchronous execution tasks
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected AppModule getModule() {
        return new ModuleForAppWithP2p(config);
    }

    @Override
    protected void applyInjector() {
        super.applyInjector();

        seedNode.setInjector(injector);

    }

    @Override
    protected void startApplication() {
        Cookie cookie = injector.getInstance(User.class).getCookie();
        cookie.getAsOptionalBoolean(CookieKey.CLEAN_TOR_DIR_AT_RESTART).ifPresent(wasCleanTorDirSet -> {
            if (wasCleanTorDirSet) {
                injector.getInstance(TorSetup.class).cleanupTorFiles(() -> {
                    log.info("Tor directory reset");
                    cookie.remove(CookieKey.CLEAN_TOR_DIR_AT_RESTART);
                }, log::error);
            }
        });

        seedNode.startApplication();

        injector.getInstance(P2PService.class).addP2PServiceListener(new P2PServiceListener() {
            @Override
            public void onDataReceived() {
                // Do nothing
            }

            @Override
            public void onNoSeedNodeAvailable() {
                // Do nothing
            }

            @Override
            public void onNoPeersAvailable() {
                // Do nothing
            }

            @Override
            public void onUpdatedDataReceived() {
                // Do nothing
            }

            @Override
            public void onTorNodeReady() {
                // Do nothing
            }

            @Override
            public void onHiddenServicePublished() {
                boolean preventPeriodicShutdownAtSeedNode = injector.getInstance(Key.get(boolean.class,
                        Names.named(Config.PREVENT_PERIODIC_SHUTDOWN_AT_SEED_NODE)));
                if (!preventPeriodicShutdownAtSeedNode) {
                    startShutDownInterval(SeedNodeMain.this);
                }
                UserThread.runAfter(() -> setupConnectionLossCheck(), 60);
            }

            @Override
            public void onSetupFailed(Throwable throwable) {
                // Do nothing
            }

            @Override
            public void onRequestCustomBridges() {
                // Do nothing
            }
        });
    }

    private void setupConnectionLossCheck() {
        // For dev testing (usually on TSK_LOCAL) we don't want to get the seed shut
        // down as it is normal that the seed is the only actively running node.
        if (Config.baseCurrencyNetwork() != BaseCurrencyNetwork.TSK_MAINNET) {
            return;
        }

        if (checkConnectionLossTime != null) {
            return;
        }

        checkConnectionLossTime = UserThread.runPeriodically(() -> {
            if (injector.getInstance(PeerManager.class).getNumAllConnectionsLostEvents() > 1) {
                // We set a flag to clear tor cache files at re-start. We cannot clear it now as Tor is used and
                // that can cause problems.
                injector.getInstance(User.class).getCookie().putAsBoolean(CookieKey.CLEAN_TOR_DIR_AT_RESTART, true);
                shutDown(this);
            }
        }, CHECK_CONNECTION_LOSS_SEC);

    }

    @Override
    public void gracefulShutDown(ResultHandler resultHandler) {
        seedNode.shutDown();
        super.gracefulShutDown(resultHandler);
    }
}
