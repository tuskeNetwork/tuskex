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

package tuskex.desktop.main.portfolio.editoffer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.common.handlers.ResultHandler;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.offer.OfferUtil;
import tuskex.core.offer.OpenOffer;
import tuskex.core.payment.validation.FiatVolumeValidator;
import tuskex.core.payment.validation.SecurityDepositValidator;
import tuskex.core.payment.validation.TskValidator;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.user.Preferences;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.PriceUtil;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.validation.AmountValidator4Decimals;
import tuskex.core.util.validation.AmountValidator8Decimals;
import tuskex.desktop.Navigation;
import tuskex.desktop.main.offer.MutableOfferViewModel;

class EditOfferViewModel extends MutableOfferViewModel<EditOfferDataModel> {

    @Inject
    public EditOfferViewModel(EditOfferDataModel dataModel,
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

    @Override
    public void activate() {
        super.activate();

        dataModel.populateData();

        long triggerPriceAsLong = dataModel.getTriggerPrice();
        dataModel.setTriggerPrice(triggerPriceAsLong);
        if (triggerPriceAsLong > 0) {
            triggerPrice.set(PriceUtil.formatMarketPrice(triggerPriceAsLong, dataModel.getCurrencyCode()));
        } else {
            triggerPrice.set("");
        }
        onTriggerPriceTextFieldChanged();
        onReserveExactAmountCheckboxChanged();
    }

    public void applyOpenOffer(OpenOffer openOffer) {
        dataModel.reset();
        dataModel.applyOpenOffer(openOffer);
    }

    public void onStartEditOffer(ErrorMessageHandler errorMessageHandler) {
        dataModel.onStartEditOffer(errorMessageHandler);
    }

    public void onPublishOffer(ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        dataModel.onPublishOffer(resultHandler, errorMessageHandler);
    }

    public void onCancelEditOffer(ErrorMessageHandler errorMessageHandler) {
        dataModel.onCancelEditOffer(errorMessageHandler);
    }

    public void onInvalidateMarketPriceMarginPct() {
        marketPriceMargin.set(FormattingUtils.formatToPercent(dataModel.getMarketPriceMarginPct()));
    }

    public void onInvalidatePrice() {
        price.set(FormattingUtils.formatPrice(null));
        price.set(FormattingUtils.formatPrice(dataModel.getPrice().get()));
    }

    public boolean isSecurityDepositValid() {
        return securityDepositValidator.validate(buyerSecurityDeposit.get()).isValid;
    }

    @Override
    public void triggerFocusOutOnAmountFields() {
        // do not update BTC Amount or minAmount here
        // issue 2798: "after a few edits of offer the BTC amount has increased"
    }
}
