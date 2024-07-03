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

package tuskex.desktop.main.offer.offerbook;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.common.config.Config;
import tuskex.core.account.sign.SignedWitnessService;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.alert.PrivateNotificationManager;
import tuskex.core.locale.Res;
import tuskex.core.offer.OfferDirection;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.desktop.Navigation;
import tuskex.desktop.common.view.FxmlView;
import tuskex.desktop.main.overlays.windows.OfferDetailsWindow;
import javafx.scene.layout.GridPane;

@FxmlView
public class TopCryptoOfferBookView extends OfferBookView<GridPane, TopCryptoOfferBookViewModel> {

    @Inject
    TopCryptoOfferBookView(TopCryptoOfferBookViewModel model,
                            Navigation navigation,
                            OfferDetailsWindow offerDetailsWindow,
                            @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter formatter,
                            PrivateNotificationManager privateNotificationManager,
                            @Named(Config.USE_DEV_PRIVILEGE_KEYS) boolean useDevPrivilegeKeys,
                            AccountAgeWitnessService accountAgeWitnessService,
                            SignedWitnessService signedWitnessService) {
        super(model, navigation, offerDetailsWindow, formatter, privateNotificationManager, useDevPrivilegeKeys, accountAgeWitnessService, signedWitnessService);
    }

    @Override
    protected String getMarketTitle() {
        return model.getDirection().equals(OfferDirection.BUY) ?
                Res.get("offerbook.availableOffersToBuy", TopCryptoOfferBookViewModel.TOP_CRYPTO.getCode(), Res.getBaseCurrencyCode()) :
                Res.get("offerbook.availableOffersToSell", TopCryptoOfferBookViewModel.TOP_CRYPTO.getCode(), Res.getBaseCurrencyCode());
    }

    @Override
    protected void activate() {
        model.onSetTradeCurrency(TopCryptoOfferBookViewModel.TOP_CRYPTO);

        super.activate();

        currencyComboBoxContainer.setVisible(false);
        currencyComboBoxContainer.setManaged(false);
    }

    @Override
    String getTradeCurrencyCode() {
        return TopCryptoOfferBookViewModel.TOP_CRYPTO.getCode();
    }
}
