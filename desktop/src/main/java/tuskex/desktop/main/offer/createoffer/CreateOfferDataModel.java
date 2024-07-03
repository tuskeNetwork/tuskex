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

package tuskex.desktop.main.offer.createoffer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.offer.CreateOfferService;
import tuskex.core.offer.OfferUtil;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.trade.statistics.TradeStatisticsManager;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.Navigation;
import tuskex.desktop.main.offer.MutableOfferDataModel;
import tuskex.network.p2p.P2PService;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain for that UI element.
 * Note that the create offer domain has a deeper scope in the application domain (TradeManager).
 * That model is just responsible for the domain specific parts displayed needed in that UI element.
 */
class CreateOfferDataModel extends MutableOfferDataModel {

    @Inject
    public CreateOfferDataModel(CreateOfferService createOfferService,
                                OpenOfferManager openOfferManager,
                                OfferUtil offerUtil,
                                TskWalletService tskWalletService,
                                Preferences preferences,
                                User user,
                                P2PService p2PService,
                                PriceFeedService priceFeedService,
                                AccountAgeWitnessService accountAgeWitnessService,
                                @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter btcFormatter,
                                TradeStatisticsManager tradeStatisticsManager,
                                Navigation navigation) {
        super(createOfferService,
                openOfferManager,
                offerUtil,
                tskWalletService,
                preferences,
                user,
                p2PService,
                priceFeedService,
                accountAgeWitnessService,
                btcFormatter,
                tradeStatisticsManager,
                navigation);
    }

    @Override
    protected Set<PaymentAccount> getUserPaymentAccounts() {
        return Objects.requireNonNull(user.getPaymentAccounts()).stream()
                .collect(Collectors.toSet());
    }
}
