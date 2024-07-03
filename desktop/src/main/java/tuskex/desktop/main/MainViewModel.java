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

package tuskex.desktop.main;

import com.google.inject.Inject;
import tuskex.common.Timer;
import tuskex.common.UserThread;
import tuskex.common.app.DevEnv;
import tuskex.common.app.Version;
import tuskex.common.config.BaseCurrencyNetwork;
import tuskex.common.config.Config;
import tuskex.common.file.CorruptedStorageFileHandler;
import tuskex.common.util.Tuple2;
import tuskex.core.account.sign.SignedWitnessService;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.alert.PrivateNotificationManager;
import tuskex.core.api.TskConnectionService;
import tuskex.core.app.TuskexSetup;
import tuskex.core.locale.CryptoCurrency;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.Res;
import tuskex.core.offer.OpenOffer;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.payment.AliPayAccount;
import tuskex.core.payment.AmazonGiftCardAccount;
import tuskex.core.payment.CryptoCurrencyAccount;
import tuskex.core.payment.RevolutAccount;
import tuskex.core.presentation.BalancePresentation;
import tuskex.core.presentation.SupportTicketsPresentation;
import tuskex.core.presentation.TradePresentation;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.trade.ArbitratorTrade;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.trade.TradeManager;
import tuskex.core.user.DontShowAgainLookup;
import tuskex.core.user.Preferences;
import tuskex.core.user.Preferences.UseTorForTsk;
import tuskex.core.user.User;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.Navigation;
import tuskex.desktop.common.model.ViewModel;
import tuskex.desktop.components.TxIdTextField;
import tuskex.desktop.main.account.AccountView;
import tuskex.desktop.main.account.content.backup.BackupView;
import tuskex.desktop.main.overlays.Overlay;
import tuskex.desktop.main.overlays.notifications.NotificationCenter;
import tuskex.desktop.main.overlays.popups.Popup;
import tuskex.desktop.main.overlays.windows.DisplayAlertMessageWindow;
import tuskex.desktop.main.overlays.windows.TacWindow;
import tuskex.desktop.main.overlays.windows.TorNetworkSettingsWindow;
import tuskex.desktop.main.overlays.windows.UpdateAmazonGiftCardAccountWindow;
import tuskex.desktop.main.overlays.windows.UpdateRevolutAccountWindow;
import tuskex.desktop.main.overlays.windows.WalletPasswordWindow;
import tuskex.desktop.main.overlays.windows.downloadupdate.DisplayUpdateDownloadWindow;
import tuskex.desktop.main.presentation.AccountPresentation;
import tuskex.desktop.main.presentation.MarketPricePresentation;
import tuskex.desktop.main.presentation.SettingsPresentation;
import tuskex.desktop.main.shared.PriceFeedComboBoxItem;
import tuskex.desktop.util.DisplayUtils;
import tuskex.desktop.util.GUIUtil;
import tuskex.network.p2p.BootstrapListener;
import tuskex.network.p2p.P2PService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MainViewModel implements ViewModel, TuskexSetup.TuskexSetupListener {
    private final TuskexSetup tuskexSetup;
    private final TskConnectionService tskConnectionService;
    private final User user;
    private final BalancePresentation balancePresentation;
    private final TradePresentation tradePresentation;
    private final SupportTicketsPresentation supportTicketsPresentation;
    private final MarketPricePresentation marketPricePresentation;
    private final AccountPresentation accountPresentation;
    private final SettingsPresentation settingsPresentation;
    private final P2PService p2PService;
    private final TradeManager tradeManager;
    private final OpenOfferManager openOfferManager;
    @Getter
    private final Preferences preferences;
    private final PrivateNotificationManager privateNotificationManager;
    private final WalletPasswordWindow walletPasswordWindow;
    private final NotificationCenter notificationCenter;
    private final TacWindow tacWindow;
    @Getter
    private final PriceFeedService priceFeedService;
    private final Config config;
    private final AccountAgeWitnessService accountAgeWitnessService;
    @Getter
    private final TorNetworkSettingsWindow torNetworkSettingsWindow;
    private final CorruptedStorageFileHandler corruptedStorageFileHandler;
    private final Navigation navigation;

    @Getter
    private final BooleanProperty showAppScreen = new SimpleBooleanProperty();
    private final DoubleProperty combinedSyncProgress = new SimpleDoubleProperty(-1);
    private final BooleanProperty isSplashScreenRemoved = new SimpleBooleanProperty();
    private final StringProperty footerVersionInfo = new SimpleStringProperty();
    private Timer checkNumberOfTskPeersTimer;
    private Timer checkNumberOfP2pNetworkPeersTimer;
    @SuppressWarnings("FieldCanBeLocal")
    private MonadicBinding<Boolean> tradesAndUIReady;
    private final Queue<Overlay<?>> popupQueue = new PriorityQueue<>(Comparator.comparing(Overlay::getDisplayOrderPriority));


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public MainViewModel(TuskexSetup tuskexSetup,
                         TskConnectionService tskConnectionService,
                         TskWalletService tskWalletService,
                         User user,
                         BalancePresentation balancePresentation,
                         TradePresentation tradePresentation,
                         SupportTicketsPresentation supportTicketsPresentation,
                         MarketPricePresentation marketPricePresentation,
                         AccountPresentation accountPresentation,
                         SettingsPresentation settingsPresentation,
                         P2PService p2PService,
                         TradeManager tradeManager,
                         OpenOfferManager openOfferManager,
                         Preferences preferences,
                         PrivateNotificationManager privateNotificationManager,
                         WalletPasswordWindow walletPasswordWindow,
                         NotificationCenter notificationCenter,
                         TacWindow tacWindow,
                         PriceFeedService priceFeedService,
                         Config config,
                         AccountAgeWitnessService accountAgeWitnessService,
                         TorNetworkSettingsWindow torNetworkSettingsWindow,
                         CorruptedStorageFileHandler corruptedStorageFileHandler,
                         Navigation navigation) {
        this.tuskexSetup = tuskexSetup;
        this.tskConnectionService = tskConnectionService;
        this.user = user;
        this.balancePresentation = balancePresentation;
        this.tradePresentation = tradePresentation;
        this.supportTicketsPresentation = supportTicketsPresentation;
        this.marketPricePresentation = marketPricePresentation;
        this.accountPresentation = accountPresentation;
        this.settingsPresentation = settingsPresentation;
        this.p2PService = p2PService;
        this.tradeManager = tradeManager;
        this.openOfferManager = openOfferManager;
        this.preferences = preferences;
        this.privateNotificationManager = privateNotificationManager;
        this.walletPasswordWindow = walletPasswordWindow;
        this.notificationCenter = notificationCenter;
        this.tacWindow = tacWindow;
        this.priceFeedService = priceFeedService;
        this.config = config;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.torNetworkSettingsWindow = torNetworkSettingsWindow;
        this.corruptedStorageFileHandler = corruptedStorageFileHandler;
        this.navigation = navigation;

        TxIdTextField.setPreferences(preferences);

        TxIdTextField.setTskWalletService(tskWalletService);

        GUIUtil.setPreferences(preferences);

        setupHandlers();
        tuskexSetup.addTuskexSetupListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // TuskexSetupListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onSetupComplete() {
        // We handle the trade period here as we display a global popup if we reached dispute time
        tradesAndUIReady = EasyBind.combine(isSplashScreenRemoved, tradeManager.persistedTradesInitializedProperty(),
                (a, b) -> a && b);
        tradesAndUIReady.subscribe((observable, oldValue, newValue) -> {
            if (newValue) {
                tradeManager.applyTradePeriodState();

                tradeManager.getOpenTrades().forEach(trade -> {

                    // check initialization error
                    if (trade.getInitError() != null) {
                        new Popup().warning("Error initializing trade" + " " + trade.getShortId() + "\n\n" +
                                trade.getInitError().getMessage())
                                .show();
                    }

                    // check trade period
                    Date maxTradePeriodDate = trade.getMaxTradePeriodDate();
                    String key;
                    switch (trade.getPeriodState()) {
                        case FIRST_HALF:
                            break;
                        case SECOND_HALF:
                            key = "displayHalfTradePeriodOver" + trade.getId();
                            if (DontShowAgainLookup.showAgain(key)) {
                                DontShowAgainLookup.dontShowAgain(key, true);
                                if (trade instanceof ArbitratorTrade) break; // skip popup if arbitrator trade
                                new Popup().warning(Res.get("popup.warning.tradePeriod.halfReached",
                                        trade.getShortId(),
                                        DisplayUtils.formatDateTime(maxTradePeriodDate)))
                                        .show();
                            }
                            break;
                        case TRADE_PERIOD_OVER:
                            key = "displayTradePeriodOver" + trade.getId();
                            if (DontShowAgainLookup.showAgain(key)) {
                                DontShowAgainLookup.dontShowAgain(key, true);
                                if (trade instanceof ArbitratorTrade) break; // skip popup if arbitrator trade
                                new Popup().warning(Res.get("popup.warning.tradePeriod.ended",
                                        trade.getShortId(),
                                        DisplayUtils.formatDateTime(maxTradePeriodDate)))
                                        .show();
                            }
                            break;
                    }
                });
            }
        });

        setupP2PNumPeersWatcher();

        marketPricePresentation.setup();
        accountPresentation.setup();
        settingsPresentation.setup();

        if (DevEnv.isDevMode()) {
            preferences.setShowOwnOffersInOfferBook(true);
            setupDevDummyPaymentAccounts();
        }

        UserThread.execute(() -> getShowAppScreen().set(true));

        // show welcome message 
        if (Config.baseCurrencyNetwork() == BaseCurrencyNetwork.TSK_STAGENET) {
            String key = "welcome.stagenet";
            if (DontShowAgainLookup.showAgain(key)) {
                UserThread.runAfter(() -> {
                    new Popup().attention(Res.get("popup.attention.welcome.stagenet")).
                            dontShowAgainId(key)
                            .closeButtonText(Res.get("shared.iUnderstand"))
                            .show();
                }, 1);
            }
        } else if (Config.baseCurrencyNetwork() == BaseCurrencyNetwork.TSK_MAINNET) {
            String key = "welcome.mainnet";
            boolean isReleaseLimited = TuskexUtils.isReleasedWithinDays(TuskexUtils.RELEASE_LIMIT_DAYS);
            if (DontShowAgainLookup.showAgain(key)) {
                UserThread.runAfter(() -> {
                    new Popup().attention(Res.get(isReleaseLimited ? "popup.attention.welcome.mainnet.test" : "popup.attention.welcome.mainnet")).
                            dontShowAgainId(key)
                            .closeButtonText(Res.get("shared.iUnderstand"))
                            .show();
                }, 1);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI handlers
    ///////////////////////////////////////////////////////////////////////////////////////////

    // After showAppScreen is set and splash screen is faded out
    void onSplashScreenRemoved() {
        isSplashScreenRemoved.set(true);

        // Delay that as we want to know what is the current path of the navigation which is set
        // in MainView showAppScreen handler
        notificationCenter.onAllServicesAndViewsInitialized();

        maybeShowPopupsFromQueue();
    }

    void onOpenDownloadWindow() {
        tuskexSetup.displayAlertIfPresent(user.getDisplayedAlert(), true);
    }

    void setPriceFeedComboBoxItem(PriceFeedComboBoxItem item) {
        marketPricePresentation.setPriceFeedComboBoxItem(item);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void setupHandlers() {
        tuskexSetup.setDisplayTacHandler(acceptedHandler -> UserThread.runAfter(() -> {
            //noinspection FunctionalExpressionCanBeFolded
            tacWindow.onAction(acceptedHandler::run).show();
        }, 1));

        tuskexSetup.setDisplayTorNetworkSettingsHandler(show -> {
            if (show) {
                torNetworkSettingsWindow.show();
            } else if (torNetworkSettingsWindow.isDisplayed()) {
                torNetworkSettingsWindow.hide();
            }
        });

        tuskexSetup.setChainFileLockedExceptionHandler(msg -> new Popup().warning(msg)
                .useShutDownButton()
                .show());
        tradeManager.setLockedUpFundsHandler(msg -> new Popup().width(850).warning(msg).show());

        tuskexSetup.setDisplayUpdateHandler((alert, key) -> new DisplayUpdateDownloadWindow(alert, config)
                .actionButtonText(Res.get("displayUpdateDownloadWindow.button.downloadLater"))
                .onAction(() -> {
                    preferences.dontShowAgain(key, false); // update later
                })
                .closeButtonText(Res.get("shared.cancel"))
                .onClose(() -> {
                    preferences.dontShowAgain(key, true); // ignore update
                })
                .show());
        tuskexSetup.setDisplayAlertHandler(alert -> new DisplayAlertMessageWindow()
                .alertMessage(alert)
                .closeButtonText(Res.get("shared.close"))
                .onClose(() -> user.setDisplayedAlert(alert))
                .show());
        tuskexSetup.setDisplayPrivateNotificationHandler(privateNotification ->
                new Popup().headLine(Res.get("popup.privateNotification.headline"))
                        .attention(privateNotification.getMessage())
                        .onClose(privateNotificationManager::removePrivateNotification)
                        .useIUnderstandButton()
                        .show());
        tuskexSetup.setDisplaySecurityRecommendationHandler(key -> {});
        tuskexSetup.setDisplayLocalhostHandler(key -> {
            if (!DevEnv.isDevMode()) {
                Popup popup = new Popup().backgroundInfo(Res.get("popup.tskLocalNode.msg"))
                        .dontShowAgainId(key);
                popup.setDisplayOrderPriority(5);
                popupQueue.add(popup);
            }
        });
        tuskexSetup.setDisplaySignedByArbitratorHandler(key -> accountPresentation.showOneTimeAccountSigningPopup(
                key, "popup.accountSigning.signedByArbitrator"));
        tuskexSetup.setDisplaySignedByPeerHandler(key -> accountPresentation.showOneTimeAccountSigningPopup(
                key, "popup.accountSigning.signedByPeer", String.valueOf(SignedWitnessService.SIGNER_AGE_DAYS)));
        tuskexSetup.setDisplayPeerLimitLiftedHandler(key -> accountPresentation.showOneTimeAccountSigningPopup(
                key, "popup.accountSigning.peerLimitLifted"));
        tuskexSetup.setDisplayPeerSignerHandler(key -> accountPresentation.showOneTimeAccountSigningPopup(
                key, "popup.accountSigning.peerSigner"));

        tuskexSetup.setWrongOSArchitectureHandler(msg -> new Popup().warning(msg).show());

        tuskexSetup.setRejectedTxErrorMessageHandler(msg -> new Popup().width(850).warning(msg).show());

        tuskexSetup.setShowPopupIfInvalidBtcConfigHandler(this::showPopupIfInvalidBtcConfig);

        tuskexSetup.setRevolutAccountsUpdateHandler(revolutAccountList -> {
            // We copy the array as we will mutate it later
            showRevolutAccountUpdateWindow(new ArrayList<>(revolutAccountList));
        });
        tuskexSetup.setAmazonGiftCardAccountsUpdateHandler(amazonGiftCardAccountList -> {
            // We copy the array as we will mutate it later
            showAmazonGiftCardAccountUpdateWindow(new ArrayList<>(amazonGiftCardAccountList));
        });
        tuskexSetup.setOsxKeyLoggerWarningHandler(() -> { });
        tuskexSetup.setQubesOSInfoHandler(() -> {
            String key = "qubesOSSetupInfo";
            if (preferences.showAgain(key)) {
                new Popup().information(Res.get("popup.info.qubesOSSetupInfo"))
                        .closeButtonText(Res.get("shared.iUnderstand"))
                        .dontShowAgainId(key)
                        .show();
            }
        });

        tuskexSetup.setDownGradePreventionHandler(lastVersion -> {
            new Popup().warning(Res.get("popup.warn.downGradePrevention", lastVersion, Version.VERSION))
                    .useShutDownButton()
                    .hideCloseButton()
                    .show();
        });

        tuskexSetup.setTorAddressUpgradeHandler(() -> new Popup().information(Res.get("popup.info.torMigration.msg"))
                .actionButtonTextWithGoTo("navigation.account.backup")
                .onAction(() -> {
                    navigation.setReturnPath(navigation.getCurrentPath());
                    navigation.navigateTo(MainView.class, AccountView.class, BackupView.class);
                }).show());

        corruptedStorageFileHandler.getFiles().ifPresent(files -> new Popup()
                .warning(Res.get("popup.warning.incompatibleDB", files.toString(), config.appDataDir))
                .useShutDownButton()
                .show());

        tuskexSetup.getTskDaemonSyncProgress().addListener((observable, oldValue, newValue) -> updateTskDaemonSyncProgress());
        tuskexSetup.getTskWalletSyncProgress().addListener((observable, oldValue, newValue) -> updateTskWalletSyncProgress());

        tuskexSetup.setFilterWarningHandler(warning -> new Popup().warning(warning).show());

        this.footerVersionInfo.setValue("v" + Version.VERSION);
        this.getNewVersionAvailableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                this.footerVersionInfo.setValue("v" + Version.VERSION + " " + Res.get("mainView.version.update"));
            } else {
                this.footerVersionInfo.setValue("v" + Version.VERSION);
            }
        });

        if (p2PService.isBootstrapped()) {
            setupInvalidOpenOffersHandler();
        } else {
            p2PService.addP2PServiceListener(new BootstrapListener() {
                @Override
                public void onDataReceived() {
                    setupInvalidOpenOffersHandler();
                }
            });
        }
    }

    private void showRevolutAccountUpdateWindow(List<RevolutAccount> revolutAccountList) {
        if (!revolutAccountList.isEmpty()) {
            RevolutAccount revolutAccount = revolutAccountList.get(0);
            revolutAccountList.remove(0);
            new UpdateRevolutAccountWindow(revolutAccount, user).onClose(() -> {
                // We delay a bit in case we have multiple account for better UX
                UserThread.runAfter(() -> showRevolutAccountUpdateWindow(revolutAccountList), 300, TimeUnit.MILLISECONDS);
            }).show();
        }
    }

    private void showAmazonGiftCardAccountUpdateWindow(List<AmazonGiftCardAccount> amazonGiftCardAccountList) {
        if (!amazonGiftCardAccountList.isEmpty()) {
            AmazonGiftCardAccount amazonGiftCardAccount = amazonGiftCardAccountList.get(0);
            amazonGiftCardAccountList.remove(0);
            new UpdateAmazonGiftCardAccountWindow(amazonGiftCardAccount, user).onClose(() -> {
                // We delay a bit in case we have multiple account for better UX
                UserThread.runAfter(() -> showAmazonGiftCardAccountUpdateWindow(amazonGiftCardAccountList), 300, TimeUnit.MILLISECONDS);
            }).show();
        }
    }

    private void setupP2PNumPeersWatcher() {
        p2PService.getNumConnectedPeers().addListener((observable, oldValue, newValue) -> {
            int numPeers = (int) newValue;
            if ((int) oldValue > 0 && numPeers == 0) {
                // give a bit of tolerance
                if (checkNumberOfP2pNetworkPeersTimer != null)
                    checkNumberOfP2pNetworkPeersTimer.stop();

                checkNumberOfP2pNetworkPeersTimer = UserThread.runAfter(() -> {
                    // check again numPeers
                    if (p2PService.getNumConnectedPeers().get() == 0) {
                        getP2pNetworkWarnMsg().set(Res.get("mainView.networkWarning.allConnectionsLost", Res.get("shared.P2P")));
                        getP2pNetworkLabelId().set("splash-error-state-msg");
                    } else {
                        getP2pNetworkWarnMsg().set(null);
                        getP2pNetworkLabelId().set("footer-pane");
                    }
                }, 5);
            } else if ((int) oldValue == 0 && numPeers > 0) {
                if (checkNumberOfP2pNetworkPeersTimer != null)
                    checkNumberOfP2pNetworkPeersTimer.stop();

                getP2pNetworkWarnMsg().set(null);
                getP2pNetworkLabelId().set("footer-pane");
            }
        });
    }

    private void showPopupIfInvalidBtcConfig() {
        preferences.setMoneroNodesOptionOrdinal(0);
        new Popup().warning(Res.get("settings.net.warn.invalidTskConfig"))
                .hideCloseButton()
                .useShutDownButton()
                .show();
    }

    private void setupDevDummyPaymentAccounts() {
        if (user.getPaymentAccounts() != null && user.getPaymentAccounts().isEmpty()) {
            AliPayAccount aliPayAccount = new AliPayAccount();
            aliPayAccount.init();
            aliPayAccount.setAccountNr("dummy_" + new Random().nextInt(100));
            aliPayAccount.setAccountName("AliPayAccount dummy");// Don't translate only for dev
            user.addPaymentAccount(aliPayAccount);

            if (p2PService.isBootstrapped()) {
                accountAgeWitnessService.publishMyAccountAgeWitness(aliPayAccount.getPaymentAccountPayload());
            } else {
                p2PService.addP2PServiceListener(new BootstrapListener() {
                    @Override
                    public void onDataReceived() {
                        accountAgeWitnessService.publishMyAccountAgeWitness(aliPayAccount.getPaymentAccountPayload());
                    }
                });
            }

            CryptoCurrencyAccount cryptoCurrencyAccount = new CryptoCurrencyAccount();
            cryptoCurrencyAccount.init();
            cryptoCurrencyAccount.setAccountName("ETH dummy");// Don't translate only for dev
            cryptoCurrencyAccount.setAddress("0x" + new Random().nextInt(1000000));
            Optional<CryptoCurrency> eth = CurrencyUtil.getCryptoCurrency("ETH");
            eth.ifPresent(cryptoCurrencyAccount::setSingleTradeCurrency);

            user.addPaymentAccount(cryptoCurrencyAccount);
        }
    }

    private void updateTskDaemonSyncProgress() {
        final DoubleProperty tskDaemonSyncProgress = tuskexSetup.getTskDaemonSyncProgress();
        UserThread.execute(() -> {
            combinedSyncProgress.set(tskDaemonSyncProgress.doubleValue());
        });
    }
    
    private void updateTskWalletSyncProgress() {
        final DoubleProperty tskWalletSyncProgress = tuskexSetup.getTskWalletSyncProgress();
        UserThread.execute(() -> {
            combinedSyncProgress.set(tskWalletSyncProgress.doubleValue());
        });
    }

    private void setupInvalidOpenOffersHandler() {
        openOfferManager.getInvalidOffers().addListener((ListChangeListener<Tuple2<OpenOffer, String>>) c -> {
            c.next();
            if (c.wasAdded()) {
                handleInvalidOpenOffers(c.getAddedSubList());
            }
        });
        handleInvalidOpenOffers(openOfferManager.getInvalidOffers());
    }

    private void handleInvalidOpenOffers(List<? extends Tuple2<OpenOffer, String>> list) {
        list.forEach(tuple2 -> {
            String errorMsg = tuple2.second;
            OpenOffer openOffer = tuple2.first;
            new Popup().warning(errorMsg)
                    .width(1000)
                    .actionButtonText(Res.get("shared.removeOffer"))
                    .onAction(() -> {
                        openOfferManager.cancelOpenOffer(openOffer, () -> {
                            log.info("Invalid open offer with ID {} was successfully removed.", openOffer.getId());
                        }, log::error);

                    })
                    .hideCloseButton()
                    .show();
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // MainView delegate getters
    ///////////////////////////////////////////////////////////////////////////////////////////

    BooleanProperty getNewVersionAvailableProperty() {
        return tuskexSetup.getNewVersionAvailableProperty();
    }

    StringProperty getNumOpenSupportTickets() {
        return supportTicketsPresentation.getNumOpenSupportTickets();
    }

    BooleanProperty getShowOpenSupportTicketsNotification() {
        return supportTicketsPresentation.getShowOpenSupportTicketsNotification();
    }

    BooleanProperty getShowPendingTradesNotification() {
        return tradePresentation.getShowPendingTradesNotification();
    }

    StringProperty getNumPendingTrades() {
        return tradePresentation.getNumPendingTrades();
    }

    StringProperty getAvailableBalance() {
        return balancePresentation.getAvailableBalance();
    }

    StringProperty getReservedBalance() {
        return balancePresentation.getReservedBalance();
    }

    StringProperty getPendingBalance() {
        return balancePresentation.getPendingBalance();
    }


    // Wallet
    StringProperty getTskInfo() {
        final StringProperty combinedInfo = new SimpleStringProperty();
        combinedInfo.bind(tuskexSetup.getTskInfo());
        return combinedInfo;
    }

    StringProperty getCombinedFooterInfo() {
        final StringProperty combinedInfo = new SimpleStringProperty();
        combinedInfo.bind(Bindings.concat(this.footerVersionInfo, " "));
        return combinedInfo;
    }

    DoubleProperty getCombinedSyncProgress() {
        return combinedSyncProgress;
    }

    StringProperty getConnectionServiceErrorMsg() {
        return tuskexSetup.getConnectionServiceErrorMsg();
    }

    StringProperty getTopErrorMsg() {
        return tuskexSetup.getTopErrorMsg();
    }

    StringProperty getTskSplashSyncIconId() {
        return tuskexSetup.getTskSplashSyncIconId();
    }

    ObjectProperty<UseTorForTsk> getUseTorForTsk() {
        return tuskexSetup.getUseTorForTsk();
    }

    // P2P
    StringProperty getP2PNetworkInfo() {
        return tuskexSetup.getP2PNetworkInfo();
    }

    BooleanProperty getSplashP2PNetworkAnimationVisible() {
        return tuskexSetup.getSplashP2PNetworkAnimationVisible();
    }

    StringProperty getP2pNetworkWarnMsg() {
        return tuskexSetup.getP2pNetworkWarnMsg();
    }

    StringProperty getP2PNetworkIconId() {
        return tuskexSetup.getP2PNetworkIconId();
    }

    StringProperty getP2PNetworkStatusIconId() {
        return tuskexSetup.getP2PNetworkStatusIconId();
    }

    BooleanProperty getUpdatedDataReceived() {
        return tuskexSetup.getUpdatedDataReceived();
    }

    StringProperty getP2pNetworkLabelId() {
        return tuskexSetup.getP2pNetworkLabelId();
    }

    // marketPricePresentation
    ObjectProperty<PriceFeedComboBoxItem> getSelectedPriceFeedComboBoxItemProperty() {
        return marketPricePresentation.getSelectedPriceFeedComboBoxItemProperty();
    }

    BooleanProperty getIsFiatCurrencyPriceFeedSelected() {
        return marketPricePresentation.getIsFiatCurrencyPriceFeedSelected();
    }

    BooleanProperty getIsExternallyProvidedPrice() {
        return marketPricePresentation.getIsExternallyProvidedPrice();
    }

    BooleanProperty getIsPriceAvailable() {
        return marketPricePresentation.getIsPriceAvailable();
    }

    IntegerProperty getMarketPriceUpdated() {
        return marketPricePresentation.getMarketPriceUpdated();
    }

    StringProperty getMarketPrice() {
        return marketPricePresentation.getMarketPrice();
    }

    StringProperty getMarketPrice(String currencyCode) {
        return marketPricePresentation.getMarketPrice(currencyCode);
    }

    public ObservableList<PriceFeedComboBoxItem> getPriceFeedComboBoxItems() {
        return marketPricePresentation.getPriceFeedComboBoxItems();
    }

    // We keep accountPresentation support even it is not used atm. But if we add a new feature and
    // add a badge again it will be needed.
    @SuppressWarnings("unused")
    public BooleanProperty getShowAccountUpdatesNotification() {
        return accountPresentation.getShowAccountUpdatesNotification();
    }

    public BooleanProperty getShowSettingsUpdatesNotification() {
        return settingsPresentation.getShowSettingsUpdatesNotification();
    }

    private void maybeShowPopupsFromQueue() {
        if (!popupQueue.isEmpty()) {
            Overlay<?> overlay = popupQueue.poll();
            overlay.getIsHiddenProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    UserThread.runAfter(this::maybeShowPopupsFromQueue, 2);
                }
            });
            overlay.show();
        }
    }

    public String getP2pConnectionSummary() {
        return Res.get("mainView.status.connections",
                p2PService.getNetworkNode().getInboundConnectionCount(),
                p2PService.getNetworkNode().getOutboundConnectionCount());
    }
}
