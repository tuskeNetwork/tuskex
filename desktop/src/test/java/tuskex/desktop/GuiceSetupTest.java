package tuskex.desktop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import tuskex.common.ClockWatcher;
import tuskex.common.config.Config;
import tuskex.common.crypto.KeyRing;
import tuskex.common.crypto.KeyStorage;
import tuskex.common.file.CorruptedStorageFileHandler;
import tuskex.common.persistence.PersistenceManager;
import tuskex.common.proto.network.NetworkProtoResolver;
import tuskex.common.proto.persistable.PersistenceProtoResolver;
import tuskex.core.app.AvoidStandbyModeService;
import tuskex.core.app.P2PNetworkSetup;
import tuskex.core.app.TorSetup;
import tuskex.core.app.WalletAppSetup;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.Res;
import tuskex.core.network.p2p.seed.DefaultSeedNodeRepository;
import tuskex.core.notifications.MobileMessageEncryption;
import tuskex.core.notifications.MobileModel;
import tuskex.core.notifications.MobileNotificationService;
import tuskex.core.notifications.MobileNotificationValidator;
import tuskex.core.notifications.alerts.MyOfferTakenEvents;
import tuskex.core.notifications.alerts.TradeEvents;
import tuskex.core.notifications.alerts.market.MarketAlerts;
import tuskex.core.notifications.alerts.price.PriceAlert;
import tuskex.core.payment.ChargeBackRisk;
import tuskex.core.payment.TradeLimits;
import tuskex.core.proto.network.CoreNetworkProtoResolver;
import tuskex.core.proto.persistable.CorePersistenceProtoResolver;
import tuskex.core.support.dispute.arbitration.ArbitrationDisputeListService;
import tuskex.core.support.dispute.arbitration.ArbitrationManager;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorManager;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorService;
import tuskex.core.support.dispute.mediation.MediationDisputeListService;
import tuskex.core.support.dispute.mediation.MediationManager;
import tuskex.core.support.dispute.mediation.mediator.MediatorManager;
import tuskex.core.support.dispute.mediation.mediator.MediatorService;
import tuskex.core.support.traderchat.TraderChatManager;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.desktop.app.TuskexAppModule;
import tuskex.desktop.common.view.CachingViewLoader;
import tuskex.desktop.common.view.ViewLoader;
import tuskex.desktop.common.view.guice.InjectorViewFactory;
import tuskex.desktop.main.funds.transactions.DisplayedTransactionsFactory;
import tuskex.desktop.main.funds.transactions.TradableRepository;
import tuskex.desktop.main.funds.transactions.TransactionAwareTradableFactory;
import tuskex.desktop.main.funds.transactions.TransactionListItemFactory;
import tuskex.desktop.main.offer.offerbook.OfferBook;
import tuskex.desktop.main.overlays.notifications.NotificationCenter;
import tuskex.desktop.main.overlays.windows.TorNetworkSettingsWindow;
import tuskex.desktop.main.presentation.MarketPricePresentation;
import tuskex.desktop.util.Transitions;
import tuskex.network.p2p.network.BridgeAddressProvider;
import tuskex.network.p2p.seed.SeedNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiceSetupTest {

    private Injector injector;

    @BeforeEach
    public void setUp() {
        Res.setup();
        CurrencyUtil.setup();

        injector = Guice.createInjector(new TuskexAppModule(new Config()));
    }

    @Test
    public void testGuiceSetup() {
        injector.getInstance(AvoidStandbyModeService.class);
        // desktop module
        assertSingleton(OfferBook.class);
        assertSingleton(CachingViewLoader.class);
        assertSingleton(Navigation.class);
        assertSingleton(InjectorViewFactory.class);
        assertSingleton(NotificationCenter.class);
        assertSingleton(TorNetworkSettingsWindow.class);
        assertSingleton(MarketPricePresentation.class);
        assertSingleton(ViewLoader.class);
        assertSingleton(Transitions.class);
        assertSingleton(TradableRepository.class);
        assertSingleton(TransactionListItemFactory.class);
        assertSingleton(TransactionAwareTradableFactory.class);
        assertSingleton(DisplayedTransactionsFactory.class);

        // core module
//        assertSingleton(TuskexSetup.class); // this is a can of worms
//        assertSingleton(DisputeMsgEvents.class);
        assertSingleton(TorSetup.class);
        assertSingleton(P2PNetworkSetup.class);
        assertSingleton(WalletAppSetup.class);
        assertSingleton(TradeLimits.class);
        assertSingleton(KeyStorage.class);
        assertSingleton(KeyRing.class);
        assertSingleton(User.class);
        assertSingleton(ClockWatcher.class);
        assertSingleton(Preferences.class);
        assertSingleton(BridgeAddressProvider.class);
        assertSingleton(CorruptedStorageFileHandler.class);
        assertSingleton(AvoidStandbyModeService.class);
        assertSingleton(DefaultSeedNodeRepository.class);
        assertSingleton(SeedNodeRepository.class);
        assertTrue(injector.getInstance(SeedNodeRepository.class) instanceof DefaultSeedNodeRepository);
        assertSingleton(CoreNetworkProtoResolver.class);
        assertSingleton(NetworkProtoResolver.class);
        assertTrue(injector.getInstance(NetworkProtoResolver.class) instanceof CoreNetworkProtoResolver);
        assertSingleton(PersistenceProtoResolver.class);
        assertSingleton(CorePersistenceProtoResolver.class);
        assertTrue(injector.getInstance(PersistenceProtoResolver.class) instanceof CorePersistenceProtoResolver);
        assertSingleton(MobileMessageEncryption.class);
        assertSingleton(MobileNotificationService.class);
        assertSingleton(MobileNotificationValidator.class);
        assertSingleton(MobileModel.class);
        assertSingleton(MyOfferTakenEvents.class);
        assertSingleton(TradeEvents.class);
        assertSingleton(PriceAlert.class);
        assertSingleton(MarketAlerts.class);
        assertSingleton(ChargeBackRisk.class);
        assertSingleton(ArbitratorService.class);
        assertSingleton(ArbitratorManager.class);
        assertSingleton(ArbitrationManager.class);
        assertSingleton(ArbitrationDisputeListService.class);
        assertSingleton(MediatorService.class);
        assertSingleton(MediatorManager.class);
        assertSingleton(MediationManager.class);
        assertSingleton(MediationDisputeListService.class);
        assertSingleton(TraderChatManager.class);

        assertNotSingleton(PersistenceManager.class);
    }

    private void assertSingleton(Class<?> type) {
        assertSame(injector.getInstance(type), injector.getInstance(type));
    }

    private void assertNotSingleton(Class<?> type) {
        assertNotSame(injector.getInstance(type), injector.getInstance(type));
    }
}
