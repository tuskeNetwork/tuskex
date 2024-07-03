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

package tuskex.apitest.method.offer;

import tuskex.proto.grpc.OfferInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static tuskex.apitest.config.ApiTestConfig.BTC;
import static tuskex.apitest.config.ApiTestConfig.TSK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static protobuf.OfferDirection.BUY;
import static protobuf.OfferDirection.SELL;

@SuppressWarnings("ConstantConditions")
@Disabled
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreateTSKOffersTest extends AbstractOfferTest {

    private static final String MAKER_FEE_CURRENCY_CODE = BTC;

    @BeforeAll
    public static void setUp() {
        AbstractOfferTest.setUp();
        createTskPaymentAccounts();
    }

    @Test
    @Order(1)
    public void testCreateFixedPriceBuy1BTCFor200KTSKOffer() {
        // Remember alt coin trades are BTC trades.  When placing an offer, you are
        // offering to buy or sell BTC, not ETH, TSK, etc.  In this test case,
        // Alice places an offer to BUY BTC.
        var newOffer = aliceClient.createFixedPricedOffer(BUY.name(),
                TSK,
                100_000_000L,
                75_000_000L,
                "0.005",   // FIXED PRICE IN BTC FOR 1 TSK
                defaultBuyerSecurityDepositPct.get(),
                alicesTskAcct.getId());
        log.debug("Sell TSK (Buy BTC) offer:\n{}", toOfferTable.apply(newOffer));
        assertTrue(newOffer.getIsMyOffer());
        assertFalse(newOffer.getIsActivated());

        String newOfferId = newOffer.getId();
        assertNotEquals("", newOfferId);
        assertEquals(BUY.name(), newOffer.getDirection());
        assertFalse(newOffer.getUseMarketBasedPrice());
        assertEquals("0.00500000", newOffer.getPrice());
        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(75_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());

        genBtcBlockAndWaitForOfferPreparation();

        newOffer = aliceClient.getOffer(newOfferId);
        assertTrue(newOffer.getIsMyOffer());
        assertTrue(newOffer.getIsActivated());
        assertEquals(newOfferId, newOffer.getId());
        assertEquals(BUY.name(), newOffer.getDirection());
        assertFalse(newOffer.getUseMarketBasedPrice());
        assertEquals("0.00500000", newOffer.getPrice());
        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(75_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());
    }

    @Test
    @Order(2)
    public void testCreateFixedPriceSell1BTCFor200KTSKOffer() {
        // Alice places an offer to SELL BTC for TSK.
        var newOffer = aliceClient.createFixedPricedOffer(SELL.name(),
                TSK,
                100_000_000L,
                50_000_000L,
                "0.005",   // FIXED PRICE IN BTC (satoshis) FOR 1 TSK
                defaultBuyerSecurityDepositPct.get(),
                alicesTskAcct.getId());
        log.debug("Buy TSK (Sell BTC) offer:\n{}", toOfferTable.apply(newOffer));
        assertTrue(newOffer.getIsMyOffer());
        assertFalse(newOffer.getIsActivated());

        String newOfferId = newOffer.getId();
        assertNotEquals("", newOfferId);
        assertEquals(SELL.name(), newOffer.getDirection());
        assertFalse(newOffer.getUseMarketBasedPrice());
        assertEquals("0.00500000", newOffer.getPrice());
        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(50_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());

        genBtcBlockAndWaitForOfferPreparation();

        newOffer = aliceClient.getOffer(newOfferId);
        assertTrue(newOffer.getIsMyOffer());
        assertTrue(newOffer.getIsActivated());
        assertEquals(newOfferId, newOffer.getId());
        assertEquals(SELL.name(), newOffer.getDirection());
        assertFalse(newOffer.getUseMarketBasedPrice());
        assertEquals("0.00500000", newOffer.getPrice());
        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(50_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());
    }

    @Test
    @Order(3)
    public void testCreatePriceMarginBasedBuy1BTCOfferWithTriggerPrice() {
        double priceMarginPctInput = 1.00;
        double mktPriceAsDouble = aliceClient.getBtcPrice(TSK);
        String triggerPrice = calcPriceAsString(mktPriceAsDouble, Double.parseDouble("-0.001"), 8);
        var newOffer = aliceClient.createMarketBasedPricedOffer(BUY.name(),
                TSK,
                100_000_000L,
                75_000_000L,
                priceMarginPctInput,
                defaultBuyerSecurityDepositPct.get(),
                alicesTskAcct.getId(),
                triggerPrice);
        log.debug("Pending Sell TSK (Buy BTC) offer:\n{}", toOfferTable.apply(newOffer));
        assertTrue(newOffer.getIsMyOffer());
        assertFalse(newOffer.getIsActivated());

        String newOfferId = newOffer.getId();
        assertNotEquals("", newOfferId);
        assertEquals(BUY.name(), newOffer.getDirection());
        assertTrue(newOffer.getUseMarketBasedPrice());

        // There is no trigger price while offer is pending.
        assertEquals(NO_TRIGGER_PRICE, newOffer.getTriggerPrice());

        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(75_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());

        genBtcBlockAndWaitForOfferPreparation();

        newOffer = aliceClient.getOffer(newOfferId);
        log.debug("Available Sell TSK (Buy BTC) offer:\n{}", toOfferTable.apply(newOffer));
        assertTrue(newOffer.getIsMyOffer());
        assertTrue(newOffer.getIsActivated());
        assertEquals(newOfferId, newOffer.getId());
        assertEquals(BUY.name(), newOffer.getDirection());
        assertTrue(newOffer.getUseMarketBasedPrice());

        // The trigger price should exist on the prepared offer.
        assertEquals(triggerPrice, newOffer.getTriggerPrice());

        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(75_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());
    }

    @Test
    @Order(4)
    public void testCreatePriceMarginBasedSell1BTCOffer() {
        // Alice places an offer to SELL BTC for TSK.
        double priceMarginPctInput = 0.50;
        var newOffer = aliceClient.createMarketBasedPricedOffer(SELL.name(),
                TSK,
                100_000_000L,
                50_000_000L,
                priceMarginPctInput,
                defaultBuyerSecurityDepositPct.get(),
                alicesTskAcct.getId(),
                NO_TRIGGER_PRICE);
        log.debug("Buy TSK (Sell BTC) offer:\n{}", toOfferTable.apply(newOffer));
        assertTrue(newOffer.getIsMyOffer());
        assertFalse(newOffer.getIsActivated());

        String newOfferId = newOffer.getId();
        assertNotEquals("", newOfferId);
        assertEquals(SELL.name(), newOffer.getDirection());
        assertTrue(newOffer.getUseMarketBasedPrice());
        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(50_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());

        genBtcBlockAndWaitForOfferPreparation();

        newOffer = aliceClient.getOffer(newOfferId);
        assertTrue(newOffer.getIsMyOffer());
        assertTrue(newOffer.getIsActivated());
        assertEquals(newOfferId, newOffer.getId());
        assertEquals(SELL.name(), newOffer.getDirection());
        assertTrue(newOffer.getUseMarketBasedPrice());
        assertEquals(100_000_000L, newOffer.getAmount());
        assertEquals(50_000_000L, newOffer.getMinAmount());
        assertEquals(.15, newOffer.getBuyerSecurityDepositPct());
        assertEquals(.15, newOffer.getSellerSecurityDepositPct());
        assertEquals(alicesTskAcct.getId(), newOffer.getPaymentAccountId());
        assertEquals(TSK, newOffer.getBaseCurrencyCode());
        assertEquals(BTC, newOffer.getCounterCurrencyCode());
    }

    @Test
    @Order(5)
    public void testGetAllMyTSKOffers() {
        List<OfferInfo> offers = aliceClient.getMyOffersSortedByDate(TSK);
        log.debug("All of Alice's TSK offers:\n{}", toOffersTable.apply(offers));
        assertEquals(4, offers.size());
        log.debug("Alice's balances\n{}", formatBalancesTbls(aliceClient.getBalances()));
    }

    @Test
    @Order(6)
    public void testGetAvailableTSKOffers() {
        List<OfferInfo> offers = bobClient.getOffersSortedByDate(TSK);
        log.debug("All of Bob's available TSK offers:\n{}", toOffersTable.apply(offers));
        assertEquals(4, offers.size());
        log.debug("Bob's balances\n{}", formatBalancesTbls(bobClient.getBalances()));
    }

    private void genBtcBlockAndWaitForOfferPreparation() {
        genBtcBlocksThenWait(1, 5000);
    }
}
