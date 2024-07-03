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
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.offer.OfferDirection;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.user.Preferences;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.desktop.Navigation;
import tuskex.desktop.common.view.FxmlView;
import tuskex.desktop.main.offer.MutableOfferView;
import tuskex.desktop.main.offer.OfferView;
import tuskex.desktop.main.overlays.windows.OfferDetailsWindow;
import tuskex.desktop.util.GUIUtil;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@FxmlView
public class CreateOfferView extends MutableOfferView<CreateOfferViewModel> {

    @Inject
    private CreateOfferView(CreateOfferViewModel model,
                            Navigation navigation,
                            Preferences preferences,
                            OfferDetailsWindow offerDetailsWindow,
                            @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter btcFormatter) {
        super(model, navigation, preferences, offerDetailsWindow, btcFormatter);
    }

    @Override
    public void initWithData(OfferDirection direction,
                             TradeCurrency tradeCurrency,
                             OfferView.OfferActionHandler offerActionHandler) {
        super.initWithData(direction, tradeCurrency, offerActionHandler);
    }

    @Override
    protected ObservableList<PaymentAccount> filterPaymentAccounts(ObservableList<PaymentAccount> paymentAccounts) {
        return FXCollections.observableArrayList(
                paymentAccounts.stream().filter(paymentAccount -> {
                    if (model.getTradeCurrency().equals(GUIUtil.TOP_CRYPTO)) {
                        return Objects.equals(paymentAccount.getSingleTradeCurrency(), GUIUtil.TOP_CRYPTO);
                    } else if (CurrencyUtil.isFiatCurrency(model.getTradeCurrency().getCode())) {
                        return paymentAccount.isFiat();
                    } else {
                        return !paymentAccount.isFiat() && !Objects.equals(paymentAccount.getSingleTradeCurrency(), GUIUtil.TOP_CRYPTO);
                    }
                }).collect(Collectors.toList()));
    }
}
