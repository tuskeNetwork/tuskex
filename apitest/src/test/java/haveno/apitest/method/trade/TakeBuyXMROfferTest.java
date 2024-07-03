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

import static tuskex.apitest.config.ApiTestConfig.TSK;
import static tuskex.cli.table.builder.TableType.OFFER_TBL;
import static tuskex.core.trade.Trade.Phase.PAYMENT_RECEIVED;
import static tuskex.core.trade.Trade.State.SELLER_SAW_ARRIVED_PAYMENT_RECEIVED_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static protobuf.Offer.State.OFFER_FEE_RESERVED;
import static protobuf.OfferDirection.SELL;

@Disabled
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TakeBuyTSKOfferTest extends AbstractTradeTest {

    // Alice is maker / tsk buyer (btc seller), Bob is taker / tsk seller (btc buyer).

    @BeforeAll
    public static void setUp() {
        AbstractOfferTest.setUp();
        createTskPaymentAccounts();
        EXPECTED_PROTOCOL_STATUS.init();
    }

    @Test
    @Order(1)
    public void testTakeAlicesSellBTCForTSKOffer(final TestInfo testInfo) {
        try {
            // Alice is going to BUY TSK, but the Offer direction = SELL because it is a
            // BTC trade;  Alice will SELL BTC for TSK.  Bob will send Alice TSK.
            // Confused me, but just need to remember there are only BTC offers.
            var btcTradeDirection = SELL.name();
            var alicesOffer = aliceClient.createFixedPricedOffer(btcTradeDirection,
                    TSK,
                    15_000_000L,
                    7_500_000L,
                    "0.00455500",   // FIXED PRICE IN BTC (satoshis) FOR 1 TSK
                    defaultBuyerSecurityDepositPct.get(),
                    alicesTskAcct.getId());
            log.debug("Alice's BUY TSK (SELL BTC) Offer:\n{}", new TableBuilder(OFFER_TBL, alicesOffer).build());
            genBtcBlocksThenWait(1, 5000);
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
            logTrade(log, testInfo, "Alice's Maker/Buyer View", aliceClient.getTrade(tradeId));
            logTrade(log, testInfo, "Bob's Taker/Seller View", bobClient.getTrade(tradeId));
        } catch (StatusRuntimeException e) {
            fail(e);
        }
    }

    @Test
    @Order(2)
    public void testBobsConfirmPaymentSent(final TestInfo testInfo) {
        try {
            var trade = bobClient.getTrade(tradeId);

            verifyTakerDepositConfirmed(trade);
            log.debug("Bob sends TSK payment to Alice for trade {}", trade.getTradeId());
            bobClient.confirmPaymentSent(trade.getTradeId());
            sleep(3500);
            waitForBuyerSeesPaymentInitiatedMessage(log, testInfo, bobClient, tradeId);

            logTrade(log, testInfo, "Alice's Maker/Buyer View (Payment Sent)", aliceClient.getTrade(tradeId));
            logTrade(log, testInfo, "Bob's Taker/Seller View (Payment Sent)", bobClient.getTrade(tradeId));
        } catch (StatusRuntimeException e) {
            fail(e);
        }
    }

    @Test
    @Order(3)
    public void testAlicesConfirmPaymentReceived(final TestInfo testInfo) {
        try {
            waitForSellerSeesPaymentInitiatedMessage(log, testInfo, aliceClient, tradeId);

            sleep(2_000);
            var trade = aliceClient.getTrade(tradeId);
            // If we were trading BSQ, Alice would verify payment has been sent to her
            // Tuskex wallet, but we can do no such checks for TSK payments.
            // All TSK transfers are done outside Tuskex.
            log.debug("Alice verifies TSK payment was received from Bob, for trade {}", trade.getTradeId());
            aliceClient.confirmPaymentReceived(trade.getTradeId());
            sleep(3_000);

            trade = aliceClient.getTrade(tradeId);
            assertEquals(OFFER_FEE_RESERVED.name(), trade.getOffer().getState());
            EXPECTED_PROTOCOL_STATUS.setState(SELLER_SAW_ARRIVED_PAYMENT_RECEIVED_MSG)
                    .setPhase(PAYMENT_RECEIVED)
                    .setPayoutPublished(true)
                    .setPaymentReceivedMessageSent(true);
            verifyExpectedProtocolStatus(trade);
            logTrade(log, testInfo, "Alice's Maker/Buyer View (Payment Received)", aliceClient.getTrade(tradeId));
            logTrade(log, testInfo, "Bob's Taker/Seller View (Payment Received)", bobClient.getTrade(tradeId));
        } catch (StatusRuntimeException e) {
            fail(e);
        }
    }
}
