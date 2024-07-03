package tuskex.desktop.util;

import tuskex.common.config.Config;
import tuskex.core.locale.GlobalSettings;
import tuskex.core.locale.Res;
import tuskex.core.monetary.Volume;
import tuskex.core.offer.Offer;
import tuskex.core.offer.OfferPayload;
import tuskex.core.util.VolumeUtil;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.coin.ImmutableCoinFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static tuskex.desktop.maker.OfferMaker.tskUsdOffer;
import static tuskex.desktop.maker.VolumeMaker.usdVolume;
import static tuskex.desktop.maker.VolumeMaker.volumeString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DisplayUtilsTest {
    private final CoinFormatter formatter = new ImmutableCoinFormatter(Config.baseCurrencyNetworkParameters().getMonetaryFormat());

    @BeforeEach
    public void setUp() {
        Locale.setDefault(Locale.US);
        GlobalSettings.setLocale(Locale.US);
        Res.setBaseCurrencyCode("TSK");
        Res.setBaseCurrencyName("Monero");
    }

    @Test
    public void testFormatAccountAge() {
        assertEquals("0 days", DisplayUtils.formatAccountAge(TimeUnit.HOURS.toMillis(23)));
        assertEquals("0 days", DisplayUtils.formatAccountAge(0));
        assertEquals("0 days", DisplayUtils.formatAccountAge(-1));
        assertEquals("1 day", DisplayUtils.formatAccountAge(TimeUnit.DAYS.toMillis(1)));
        assertEquals("2 days", DisplayUtils.formatAccountAge(TimeUnit.DAYS.toMillis(2)));
        assertEquals("30 days", DisplayUtils.formatAccountAge(TimeUnit.DAYS.toMillis(30)));
        assertEquals("60 days", DisplayUtils.formatAccountAge(TimeUnit.DAYS.toMillis(60)));
    }

    @Test
    public void testFormatVolume() {
        assertEquals("1", VolumeUtil.formatVolume(make(tskUsdOffer), true, 4));
        assertEquals("100", VolumeUtil.formatVolume(make(usdVolume)));
        assertEquals("1775", VolumeUtil.formatVolume(make(usdVolume.but(with(volumeString, "1774.62")))));
    }

    @Test
    public void testFormatSameVolume() {
        Offer offer = mock(Offer.class);
        Volume tsk = Volume.parse("0.10", "TSK");
        when(offer.getMinVolume()).thenReturn(tsk);
        when(offer.getVolume()).thenReturn(tsk);

        assertEquals("0.10000000", VolumeUtil.formatVolume(offer.getVolume()));
    }

    @Test
    public void testFormatDifferentVolume() {
        Offer offer = mock(Offer.class);
        Volume tskMin = Volume.parse("0.10", "TSK");
        Volume tskMax = Volume.parse("0.25", "TSK");
        when(offer.isRange()).thenReturn(true);
        when(offer.getMinVolume()).thenReturn(tskMin);
        when(offer.getVolume()).thenReturn(tskMax);

        assertEquals("0.10000000 - 0.25000000", VolumeUtil.formatVolume(offer, false, 0));
    }

    @Test
    public void testFormatNullVolume() {
        Offer offer = mock(Offer.class);
        when(offer.getMinVolume()).thenReturn(null);
        when(offer.getVolume()).thenReturn(null);

        assertEquals("", VolumeUtil.formatVolume(offer.getVolume()));
    }

    @Test
    public void testFormatSameAmount() {
        Offer offer = mock(Offer.class);
        when(offer.getMinAmount()).thenReturn(BigInteger.valueOf(100000000000L));
        when(offer.getAmount()).thenReturn(BigInteger.valueOf(100000000000L));

        assertEquals("0.10", DisplayUtils.formatAmount(offer, formatter));
    }

    @Test
    public void testFormatDifferentAmount() {
        OfferPayload offerPayload = mock(OfferPayload.class);
        Offer offer = new Offer(offerPayload);
        when(offerPayload.getMinAmount()).thenReturn(100000000000L);
        when(offerPayload.getAmount()).thenReturn(200000000000L);

        assertEquals("0.10 - 0.20", DisplayUtils.formatAmount(offer, formatter));
    }

    @Test
    public void testFormatAmountWithAlignmenWithDecimals() {
        OfferPayload offerPayload = mock(OfferPayload.class);
        Offer offer = new Offer(offerPayload);
        when(offerPayload.getMinAmount()).thenReturn(100000000000L);
        when(offerPayload.getAmount()).thenReturn(200000000000L);

        assertEquals("0.1000 - 0.2000", DisplayUtils.formatAmount(offer, 4, true, 15, formatter));
    }

    @Test
    public void testFormatAmountWithAlignmenWithDecimalsNoRange() {
        OfferPayload offerPayload = mock(OfferPayload.class);
        Offer offer = new Offer(offerPayload);
        when(offerPayload.getMinAmount()).thenReturn(100000000000L);
        when(offerPayload.getAmount()).thenReturn(100000000000L);

        assertEquals("0.1000", DisplayUtils.formatAmount(offer, 4, true, 15, formatter));
    }

    @Test
    public void testFormatNullAmount() {
        Offer offer = mock(Offer.class);
        when(offer.getMinAmount()).thenReturn(null);
        when(offer.getAmount()).thenReturn(null);

        assertEquals("", DisplayUtils.formatAmount(offer, formatter));
    }
}
