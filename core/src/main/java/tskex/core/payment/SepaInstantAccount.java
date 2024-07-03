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

package tuskex.core.payment;

import tuskex.core.api.model.PaymentAccountForm;
import tuskex.core.api.model.PaymentAccountFormField;
import tuskex.core.locale.Country;
import tuskex.core.locale.CountryUtil;
import tuskex.core.locale.TraditionalCurrency;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.payload.PaymentMethod;
import tuskex.core.payment.payload.SepaInstantAccountPayload;
import tuskex.core.payment.validation.SepaIBANValidator;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public final class SepaInstantAccount extends CountryBasedPaymentAccount implements BankAccount {

    public static final List<TradeCurrency> SUPPORTED_CURRENCIES = List.of(new TraditionalCurrency("EUR"));

    public SepaInstantAccount() {
        super(PaymentMethod.SEPA_INSTANT);
        setSingleTradeCurrency(SUPPORTED_CURRENCIES.get(0));
    }

    @Override
    protected PaymentAccountPayload createPayload() {
        return new SepaInstantAccountPayload(paymentMethod.getId(), id,
                CountryUtil.getAllSepaInstantCountries());
    }

    @Override
    public String getBankId() {
        return ((SepaInstantAccountPayload) paymentAccountPayload).getBic();
    }

    public void setHolderName(String holderName) {
        ((SepaInstantAccountPayload) paymentAccountPayload).setHolderName(holderName);
    }

    public String getHolderName() {
        return ((SepaInstantAccountPayload) paymentAccountPayload).getHolderName();
    }

    public void setIban(String iban) {
        ((SepaInstantAccountPayload) paymentAccountPayload).setIban(iban);
    }

    public String getIban() {
        return ((SepaInstantAccountPayload) paymentAccountPayload).getIban();
    }

    public void setBic(String bic) {
        ((SepaInstantAccountPayload) paymentAccountPayload).setBic(bic);
    }

    public String getBic() {
        return ((SepaInstantAccountPayload) paymentAccountPayload).getBic();
    }

    public List<String> getAcceptedCountryCodes() {
        return ((SepaInstantAccountPayload) paymentAccountPayload).getAcceptedCountryCodes();
    }

    public void addAcceptedCountry(String countryCode) {
        ((SepaInstantAccountPayload) paymentAccountPayload).addAcceptedCountry(countryCode);
    }

    public void removeAcceptedCountry(String countryCode) {
        ((SepaInstantAccountPayload) paymentAccountPayload).removeAcceptedCountry(countryCode);
    }

    @Override
    public void onPersistChanges() {
        super.onPersistChanges();
        ((SepaInstantAccountPayload) paymentAccountPayload).onPersistChanges();
    }

    @Override
    public void revertChanges() {
        super.revertChanges();
        ((SepaInstantAccountPayload) paymentAccountPayload).revertChanges();
    }

    @Override
    public @NotNull List<PaymentAccountFormField.FieldId> getInputFieldIds() {
        return SepaAccount.INPUT_FIELD_IDS;
    }

    @Override
    public @NotNull List<TradeCurrency> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    @Override
    @Nullable
    public List<Country> getSupportedCountries() {
        return CountryUtil.getAllSepaCountries();
    }

    @Override
    public void validateFormField(PaymentAccountForm form, PaymentAccountFormField.FieldId fieldId, String value) {
        switch (fieldId) {
        case IBAN:
            processValidationResult(new SepaIBANValidator().validate(value));
            break;
        default:
            super.validateFormField(form, fieldId, value);
        }
    }

    @Override
    protected PaymentAccountFormField getEmptyFormField(PaymentAccountFormField.FieldId fieldId) {
        var field = super.getEmptyFormField(fieldId);
        switch (fieldId) {
        case ACCEPTED_COUNTRY_CODES:
            field.setSupportedSepaEuroCountries(CountryUtil.getAllSepaEuroCountries());
            field.setSupportedSepaNonEuroCountries(CountryUtil.getAllSepaNonEuroCountries());
            break;
        default:
            // no action
        }
        return field;
    }
}
