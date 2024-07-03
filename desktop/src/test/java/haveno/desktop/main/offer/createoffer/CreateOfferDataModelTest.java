package tuskex.desktop.main.offer.createoffer;

import tuskex.core.locale.CryptoCurrency;
import tuskex.core.locale.TraditionalCurrency;
import tuskex.core.locale.GlobalSettings;
import tuskex.core.locale.Res;
import tuskex.core.offer.CreateOfferService;
import tuskex.core.offer.OfferDirection;
import tuskex.core.offer.OfferUtil;
import tuskex.core.payment.ZelleAccount;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.payment.RevolutAccount;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.trade.statistics.TradeStatisticsManager;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.core.tsk.wallet.TskWalletService;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateOfferDataModelTest {

    private CreateOfferDataModel model;
    private User user;
    private Preferences preferences;
    private OfferUtil offerUtil;

    @BeforeEach
    public void setUp() {
        final CryptoCurrency tsk = new CryptoCurrency("TSK", "monero");
        GlobalSettings.setDefaultTradeCurrency(tsk);
        Res.setup();

        TskAddressEntry addressEntry = mock(TskAddressEntry.class);
        TskWalletService tskWalletService = mock(TskWalletService.class);
        PriceFeedService priceFeedService = mock(PriceFeedService.class);
        CreateOfferService createOfferService = mock(CreateOfferService.class);
        preferences = mock(Preferences.class);
        offerUtil = mock(OfferUtil.class);
        user = mock(User.class);
        var tradeStats = mock(TradeStatisticsManager.class);

        when(tskWalletService.getOrCreateAddressEntry(anyString(), any())).thenReturn(addressEntry);
        when(preferences.isUsePercentageBasedPrice()).thenReturn(true);
        when(preferences.getBuyerSecurityDepositAsPercent(null)).thenReturn(0.01);
        when(createOfferService.getRandomOfferId()).thenReturn(UUID.randomUUID().toString());
        when(tradeStats.getObservableTradeStatisticsSet()).thenReturn(FXCollections.observableSet());

        model = new CreateOfferDataModel(createOfferService,
                null,
                offerUtil,
                tskWalletService,
                preferences,
                user,
                null,
                priceFeedService,
                null,
                null,
                tradeStats,
                null);
    }

    @Test
    public void testUseTradeCurrencySetInOfferViewWhenInPaymentAccountAvailable() {
        final HashSet<PaymentAccount> paymentAccounts = new HashSet<>();
        final ZelleAccount zelleAccount = new ZelleAccount();
        zelleAccount.setId("234");
        zelleAccount.setAccountName("zelleAccount");
        paymentAccounts.add(zelleAccount);
        final RevolutAccount revolutAccount = new RevolutAccount();
        revolutAccount.setId("123");
        revolutAccount.setAccountName("revolutAccount");
        revolutAccount.setSingleTradeCurrency(new TraditionalCurrency("EUR"));
        revolutAccount.addCurrency(new TraditionalCurrency("USD"));
        paymentAccounts.add(revolutAccount);

        when(user.getPaymentAccounts()).thenReturn(paymentAccounts);
        when(preferences.getSelectedPaymentAccountForCreateOffer()).thenReturn(revolutAccount);

        model.initWithData(OfferDirection.BUY, new TraditionalCurrency("USD"));
        assertEquals("USD", model.getTradeCurrencyCode().get());
    }

    @Test
    public void testUseTradeAccountThatMatchesTradeCurrencySetInOffer() {
        final HashSet<PaymentAccount> paymentAccounts = new HashSet<>();
        final ZelleAccount zelleAccount = new ZelleAccount();
        zelleAccount.setId("234");
        zelleAccount.setAccountName("zelleAccount");
        paymentAccounts.add(zelleAccount);
        final RevolutAccount revolutAccount = new RevolutAccount();
        revolutAccount.setId("123");
        revolutAccount.setAccountName("revolutAccount");
        revolutAccount.setSingleTradeCurrency(new TraditionalCurrency("EUR"));
        paymentAccounts.add(revolutAccount);

        when(user.getPaymentAccounts()).thenReturn(paymentAccounts);
        when(user.findFirstPaymentAccountWithCurrency(new TraditionalCurrency("USD"))).thenReturn(zelleAccount);
        when(preferences.getSelectedPaymentAccountForCreateOffer()).thenReturn(revolutAccount);

        model.initWithData(OfferDirection.BUY, new TraditionalCurrency("USD"));
        assertEquals("USD", model.getTradeCurrencyCode().get());
    }
}
