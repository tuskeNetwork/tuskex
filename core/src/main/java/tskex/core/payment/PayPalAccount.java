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

package tuskex.core.payment;

import tuskex.core.api.model.PaymentAccountFormField;
import tuskex.core.locale.TraditionalCurrency;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.payload.PaymentMethod;
import tuskex.core.payment.payload.PayPalAccountPayload;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public final class PayPalAccount extends PaymentAccount {

    // https://developer.paypal.com/docs/reports/reference/paypal-supported-currencies/
    public static final List<TradeCurrency> SUPPORTED_CURRENCIES = List.of(
            new TraditionalCurrency("AUD"),
            new TraditionalCurrency("BRL"),
            new TraditionalCurrency("CAD"),
            new TraditionalCurrency("CNY"),
            new TraditionalCurrency("CZK"),
            new TraditionalCurrency("DKK"),
            new TraditionalCurrency("EUR"),
            new TraditionalCurrency("HKD"),
            new TraditionalCurrency("HUF"),
            new TraditionalCurrency("ILS"),
            new TraditionalCurrency("JPY"),
            new TraditionalCurrency("MYR"),
            new TraditionalCurrency("MXN"),
            new TraditionalCurrency("TWD"),
            new TraditionalCurrency("NZD"),
            new TraditionalCurrency("NOK"),
            new TraditionalCurrency("PHP"),
            new TraditionalCurrency("PLN"),
            new TraditionalCurrency("GBP"),
            new TraditionalCurrency("SGD"),
            new TraditionalCurrency("SEK"),
            new TraditionalCurrency("CHF"),
            new TraditionalCurrency("THB"),
            new TraditionalCurrency("USD"));

    private static final List<PaymentAccountFormField.FieldId> INPUT_FIELD_IDS = List.of(
            PaymentAccountFormField.FieldId.EMAIL_OR_MOBILE_NR_OR_USERNAME,
            PaymentAccountFormField.FieldId.TRADE_CURRENCIES,
            PaymentAccountFormField.FieldId.ACCOUNT_NAME,
            PaymentAccountFormField.FieldId.SALT);

    public PayPalAccount() {
        super(PaymentMethod.PAYPAL);
    }

    @Override
    protected PaymentAccountPayload createPayload() {
        return new PayPalAccountPayload(paymentMethod.getId(), id);
    }

    @Override
    public @NonNull List<TradeCurrency> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    @Override
    public @NonNull List<PaymentAccountFormField.FieldId> getInputFieldIds() {
        return INPUT_FIELD_IDS;
    }

    public void setEmailOrMobileNrOrUsername(String emailOrMobileNrOrUsername) {
        ((PayPalAccountPayload) paymentAccountPayload)
                .setEmailOrMobileNrOrUsername(emailOrMobileNrOrUsername);
    }

    public String getEmailOrMobileNrOrUsername() {
        return ((PayPalAccountPayload) paymentAccountPayload).getEmailOrMobileNrOrUsername();
    }
}
