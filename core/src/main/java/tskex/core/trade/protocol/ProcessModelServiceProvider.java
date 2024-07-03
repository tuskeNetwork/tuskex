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

package tuskex.core.trade.protocol;

import com.google.inject.Inject;
import tuskex.common.crypto.KeyRing;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.filter.FilterManager;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorManager;
import tuskex.core.support.dispute.mediation.mediator.MediatorManager;
import tuskex.core.support.dispute.refund.refundagent.RefundAgentManager;
import tuskex.core.trade.statistics.ReferralIdService;
import tuskex.core.trade.statistics.TradeStatisticsManager;
import tuskex.core.user.User;
import tuskex.core.tsk.wallet.BtcWalletService;
import tuskex.core.tsk.wallet.TradeWalletService;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.network.p2p.P2PService;
import lombok.Getter;

@Getter
public class ProcessModelServiceProvider {
    private final OpenOfferManager openOfferManager;
    private final P2PService p2PService;
    private final BtcWalletService btcWalletService;
    private final TskWalletService tskWalletService;
    private final TradeWalletService tradeWalletService;
    private final ReferralIdService referralIdService;
    private final User user;
    private final FilterManager filterManager;
    private final AccountAgeWitnessService accountAgeWitnessService;
    private final TradeStatisticsManager tradeStatisticsManager;
    private final ArbitratorManager arbitratorManager;
    private final MediatorManager mediatorManager;
    private final RefundAgentManager refundAgentManager;
    private final KeyRing keyRing;

    @Inject
    public ProcessModelServiceProvider(OpenOfferManager openOfferManager,
                                       P2PService p2PService,
                                       BtcWalletService btcWalletService,
                                       TskWalletService tskWalletService,
                                       TradeWalletService tradeWalletService,
                                       ReferralIdService referralIdService,
                                       User user,
                                       FilterManager filterManager,
                                       AccountAgeWitnessService accountAgeWitnessService,
                                       TradeStatisticsManager tradeStatisticsManager,
                                       ArbitratorManager arbitratorManager,
                                       MediatorManager mediatorManager,
                                       RefundAgentManager refundAgentManager,
                                       KeyRing keyRing) {
        this.openOfferManager = openOfferManager;
        this.p2PService = p2PService;
        this.btcWalletService = btcWalletService;
        this.tskWalletService = tskWalletService;
        this.tradeWalletService = tradeWalletService;
        this.referralIdService = referralIdService;
        this.user = user;
        this.filterManager = filterManager;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.tradeStatisticsManager = tradeStatisticsManager;
        this.arbitratorManager = arbitratorManager;
        this.mediatorManager = mediatorManager;
        this.refundAgentManager = refundAgentManager;
        this.keyRing = keyRing;
    }
}
