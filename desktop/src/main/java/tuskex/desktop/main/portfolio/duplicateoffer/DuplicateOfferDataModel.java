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

package tuskex.desktop.main.portfolio.duplicateoffer;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.offer.CreateOfferService;
import tuskex.core.offer.Offer;
import tuskex.core.offer.OfferUtil;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.trade.statistics.TradeStatisticsManager;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.coin.CoinUtil;
import tuskex.core.tsk.wallet.Restrictions;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.Navigation;
import tuskex.desktop.main.offer.MutableOfferDataModel;
import tuskex.network.p2p.P2PService;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class DuplicateOfferDataModel extends MutableOfferDataModel {

    @Inject
    DuplicateOfferDataModel(CreateOfferService createOfferService,
                       OpenOfferManager openOfferManager,
                       OfferUtil offerUtil,
                       TskWalletService btcWalletService,
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
                btcWalletService,
                preferences,
                user,
                p2PService,
                priceFeedService,
                accountAgeWitnessService,
                btcFormatter,
                tradeStatisticsManager,
                navigation);
    }

    public void populateData(Offer offer) {
        if (offer == null)
            return;
        paymentAccount = user.getPaymentAccount(offer.getMakerPaymentAccountId());
        setMinAmount(offer.getMinAmount());
        setAmount(offer.getAmount());
        setPrice(offer.getPrice());
        setVolume(offer.getVolume());
        setUseMarketBasedPrice(offer.isUseMarketBasedPrice());

        setBuyerSecurityDeposit(getBuyerSecurityAsPercent(offer));

        if (offer.isUseMarketBasedPrice()) {
            setMarketPriceMarginPct(offer.getMarketPriceMarginPct());
        }
    }

    private double getBuyerSecurityAsPercent(Offer offer) {
        BigInteger offerBuyerSecurityDeposit = getBoundedBuyerSecurityDeposit(offer.getMaxBuyerSecurityDeposit());
        double offerBuyerSecurityDepositAsPercent = CoinUtil.getAsPercentPerBtc(offerBuyerSecurityDeposit,
                offer.getAmount());
        return Math.min(offerBuyerSecurityDepositAsPercent,
                Restrictions.getMaxBuyerSecurityDepositAsPercent());
    }

    @Override
    protected Set<PaymentAccount> getUserPaymentAccounts() {
        return Objects.requireNonNull(user.getPaymentAccounts()).stream()
                .filter(account -> !account.getPaymentMethod().isBsqSwap())
                .collect(Collectors.toSet());
    }

    @Override
    protected PaymentAccount getPreselectedPaymentAccount() {
        // If trade currency is BSQ don't use the BSQ swap payment account as it will automatically
        // close the duplicate offer view
        Optional<TradeCurrency> bsqOptional = CurrencyUtil.getTradeCurrency("BSQ");
        if (bsqOptional.isPresent() && tradeCurrency.equals(bsqOptional.get()) && user.getPaymentAccounts() != null) {
            Optional<PaymentAccount> firstBsqPaymentAccount = user.getPaymentAccounts().stream().filter(paymentAccount1 -> {
                Optional<TradeCurrency> tradeCurrency = paymentAccount1.getTradeCurrency();
                return tradeCurrency.isPresent() &&
                        tradeCurrency.get().equals(bsqOptional.get());
            }).findFirst();

            if (firstBsqPaymentAccount.isPresent()) {
                return firstBsqPaymentAccount.get();
            }
        }

        return super.getPreselectedPaymentAccount();
    }
}
