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

package tuskex.apitest.method.trade;

import tuskex.apitest.method.offer.AbstractOfferTest;
import tuskex.cli.table.builder.TableBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import static tuskex.apitest.config.ApiTestConfig.BTC;
import static tuskex.apitest.config.ApiTestConfig.TSK;
import static tuskex.cli.table.builder.TableType.OFFER_TBL;
import static tuskex.core.trade.Trade.Phase.PAYMENT_RECEIVED;
import static tuskex.core.trade.Trade.State.SELLER_SAW_ARRIVED_PAYMENT_RECEIVED_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static protobuf.OfferDirection.BUY;

@Disabled
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TakeSellTSKOfferTest extends AbstractTradeTest {

    // Alice is maker / tsk seller (btc buyer), Bob is taker / tsk buyer (btc seller).

    // Maker and Taker fees are in BTC.
    private static final String TRADE_FEE_CURRENCY_CODE = BTC;

    private static final String WITHDRAWAL_TX_MEMO = "Bob's trade withdrawal";

    @BeforeAll
    public static void setUp() {
        AbstractOfferTest.setUp();
        createTskPaymentAccounts();
        EXPECTED_PROTOCOL_STATUS.init();
    }

    @Test
    @Order(1)
    public void testTakeAlicesBuyBTCForTSKOffer(final TestInfo testInfo) {
        try {
            // Alice is going to SELL TSK, but the Offer direction = BUY because it is a
            // BTC trade;  Alice will BUY BTC for TSK.  Alice will send Bob TSK.
            // Confused me, but just need to remember there are only BTC offers.
            var btcTradeDirection = BUY.name();
            double priceMarginPctInput = 1.50;
            var alicesOffer = aliceClient.createMarketBasedPricedOffer(btcTradeDirection,
                    TSK,
                    20_000_000L,
                    10_500_000L,
                    priceMarginPctInput,
                    defaultBuyerSecurityDepositPct.get(),
                    alicesTskAcct.getId(),
                    NO_TRIGGER_PRICE);
            log.debug("Alice's SELL TSK (BUY BTC) Offer:\n{}", new TableBuilder(OFFER_TBL, alicesOffer).build());
            genBtcBlocksThenWait(1, 4000);
            var offerId = alicesOffer.getId();

            var alicesTskOffers = aliceClient.getMyOffers(btcTradeDirection, TSK);
            assertEquals(1, alicesTskOffers.size());
            var trade = takeAlicesOffer(offerId, bobsTskAcct.getId());
            alicesTskOffers = aliceClient.getMyOffersSortedByDate(TSK);
            assertEquals(0, alicesTskOffers.size());
            genBtcBlocksThenWait(1, 2_500);

            waitForDepositUnlocked(log, testInfo, bobClient, trade.getTradeId());

            trade = bobClient.getTrade(tradeId);
            verifyTakerDepositConfirmed(trade);
            logTrade(log, testInfo, "Alice's Maker/Seller View", aliceClient.getTrade(tradeId));
            logTrade(log, testInfo, "Bob's Taker/Buyer View", bobClient.getTrade(tradeId));
        } catch (StatusRuntimeException e) {
            fail(e);
        }
    }

    @Test
    @Order(2)
    public void testAlicesConfirmPaymentSent(final TestInfo testInfo) {
        try {
            var trade = aliceClient.getTrade(tradeId);
            waitForDepositUnlocked(log, testInfo, aliceClient, trade.getTradeId());
            log.debug("Alice sends TSK payment to Bob for trade {}", trade.getTradeId());
            aliceClient.confirmPaymentSent(trade.getTradeId());
            sleep(3500);

            waitForBuyerSeesPaymentInitiatedMessage(log, testInfo, aliceClient, tradeId);
            logTrade(log, testInfo, "Alice's Maker/Seller View (Payment Sent)", aliceClient.getTrade(tradeId));
            logTrade(log, testInfo, "Bob's Taker/Buyer View (Payment Sent)", bobClient.getTrade(tradeId));
        } catch (StatusRuntimeException e) {
            fail(e);
        }
    }

    @Test
    @Order(3)
    public void testBobsConfirmPaymentReceived(final TestInfo testInfo) {
        try {
            waitForSellerSeesPaymentInitiatedMessage(log, testInfo, bobClient, tradeId);

            var trade = bobClient.getTrade(tradeId);
            sleep(2_000);
            // If we were trading BTC, Bob would verify payment has been sent to his
            // Tuskex wallet, but we can do no such checks for TSK payments.
            // All TSK transfers are done outside Tuskex.
            log.debug("Bob verifies TSK payment was received from Alice, for trade {}", trade.getTradeId());
            bobClient.confirmPaymentReceived(trade.getTradeId());
            sleep(3_000);

            trade = bobClient.getTrade(tradeId);
            // Warning:  trade.getOffer().getState() might be AVAILABLE, not OFFER_FEE_RESERVED.
            EXPECTED_PROTOCOL_STATUS.setState(SELLER_SAW_ARRIVED_PAYMENT_RECEIVED_MSG)
                    .setPhase(PAYMENT_RECEIVED)
                    .setPayoutPublished(true)
                    .setPaymentReceivedMessageSent(true);
            verifyExpectedProtocolStatus(trade);
            logTrade(log, testInfo, "Alice's Maker/Seller View (Payment Received)", aliceClient.getTrade(tradeId));
            logTrade(log, testInfo, "Bob's Taker/Buyer View (Payment Received)", bobClient.getTrade(tradeId));
        } catch (StatusRuntimeException e) {
            fail(e);
        }
    }
}
