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

package tuskex.core.offer.placeoffer;

import tuskex.common.crypto.KeyRing;
import tuskex.common.taskrunner.Model;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.filter.FilterManager;
import tuskex.core.offer.OfferBookService;
import tuskex.core.offer.OpenOffer;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.offer.messages.SignOfferResponse;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorManager;
import tuskex.core.support.dispute.mediation.mediator.MediatorManager;
import tuskex.core.trade.statistics.TradeStatisticsManager;
import tuskex.core.user.User;
import tuskex.core.tsk.wallet.BtcWalletService;
import tuskex.core.tsk.wallet.TradeWalletService;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.network.p2p.P2PService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Transaction;

import java.math.BigInteger;

@Slf4j
@Getter
public class PlaceOfferModel implements Model {
    // Immutable
    private final OpenOffer openOffer;
    private final BigInteger reservedFundsForOffer;
    private final boolean useSavingsWallet;
    private final P2PService p2PService;
    private final BtcWalletService walletService;
    private final TskWalletService tskWalletService;
    private final TradeWalletService tradeWalletService;
    private final OfferBookService offerBookService;
    private final ArbitratorManager arbitratorManager;
    private final MediatorManager mediatorManager;
    private final TradeStatisticsManager tradeStatisticsManager;
    private final User user;
    private final KeyRing keyRing;
    @Getter
    private final FilterManager filterManager;
    @Getter
    private final AccountAgeWitnessService accountAgeWitnessService;
    @Getter
    private final OpenOfferManager openOfferManager;

    // Mutable
    @Setter
    private boolean offerAddedToOfferBook;
    @Setter
    private Transaction transaction;
    @Setter
    private SignOfferResponse signOfferResponse;
    @Setter
    @Getter
    protected PlaceOfferProtocol protocol;

    public PlaceOfferModel(OpenOffer openOffer,
                           BigInteger reservedFundsForOffer,
                           boolean useSavingsWallet,
                           P2PService p2PService,
                           BtcWalletService walletService,
                           TskWalletService tskWalletService,
                           TradeWalletService tradeWalletService,
                           OfferBookService offerBookService,
                           ArbitratorManager arbitratorManager,
                           MediatorManager mediatorManager,
                           TradeStatisticsManager tradeStatisticsManager,
                           User user,
                           KeyRing keyRing,
                           FilterManager filterManager,
                           AccountAgeWitnessService accountAgeWitnessService,
                           OpenOfferManager openOfferManager) {
        this.openOffer = openOffer;
        this.reservedFundsForOffer = reservedFundsForOffer;
        this.useSavingsWallet = useSavingsWallet;
        this.p2PService = p2PService;
        this.walletService = walletService;
        this.tskWalletService = tskWalletService;
        this.tradeWalletService = tradeWalletService;
        this.offerBookService = offerBookService;
        this.arbitratorManager = arbitratorManager;
        this.mediatorManager = mediatorManager;
        this.tradeStatisticsManager = tradeStatisticsManager;
        this.user = user;
        this.keyRing = keyRing;
        this.filterManager = filterManager;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.openOfferManager = openOfferManager;
    }

    @Override
    public void onComplete() {
    }
}
