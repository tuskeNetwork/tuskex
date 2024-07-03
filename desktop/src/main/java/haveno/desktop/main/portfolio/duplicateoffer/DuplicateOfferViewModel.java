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
import tuskex.core.offer.Offer;
import tuskex.core.offer.OfferPayload;
import tuskex.core.offer.OfferUtil;
import tuskex.core.payment.validation.FiatVolumeValidator;
import tuskex.core.payment.validation.SecurityDepositValidator;
import tuskex.core.payment.validation.TskValidator;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.user.Preferences;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.validation.AmountValidator4Decimals;
import tuskex.core.util.validation.AmountValidator8Decimals;
import tuskex.desktop.Navigation;
import tuskex.desktop.main.offer.MutableOfferViewModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DuplicateOfferViewModel extends MutableOfferViewModel<DuplicateOfferDataModel> {

    @Inject
    public DuplicateOfferViewModel(DuplicateOfferDataModel dataModel,
                              FiatVolumeValidator fiatVolumeValidator,
                              AmountValidator4Decimals priceValidator4Decimals,
                              AmountValidator8Decimals priceValidator8Decimals,
                              TskValidator btcValidator,
                              SecurityDepositValidator securityDepositValidator,
                              PriceFeedService priceFeedService,
                              AccountAgeWitnessService accountAgeWitnessService,
                              Navigation navigation,
                              Preferences preferences,
                              @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter btcFormatter,
                              OfferUtil offerUtil) {
        super(dataModel,
                fiatVolumeValidator,
                priceValidator4Decimals,
                priceValidator8Decimals,
                btcValidator,
                securityDepositValidator,
                priceFeedService,
                accountAgeWitnessService,
                navigation,
                preferences,
                btcFormatter,
                offerUtil);
        syncMinAmountWithAmount = false;
    }

    public void initWithData(OfferPayload offerPayload) {
        this.offer = new Offer(offerPayload);
        offer.setPriceFeedService(priceFeedService);
    }

    @Override
    public void activate() {
        super.activate();
        dataModel.populateData(offer);
        triggerFocusOutOnAmountFields();
        onFocusOutPriceAsPercentageTextField(true, false);
    }
}
