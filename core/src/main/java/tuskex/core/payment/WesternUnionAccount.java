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

import tuskex.core.api.model.PaymentAccountFormField;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.payload.PaymentMethod;
import tuskex.core.payment.payload.WesternUnionAccountPayload;
import lombok.NonNull;

import java.util.List;

public final class WesternUnionAccount extends CountryBasedPaymentAccount {

    public static final List<TradeCurrency> SUPPORTED_CURRENCIES = CurrencyUtil.getAllFiatCurrencies();

    public WesternUnionAccount() {
        super(PaymentMethod.WESTERN_UNION);
    }

    @Override
    protected PaymentAccountPayload createPayload() {
        return new WesternUnionAccountPayload(paymentMethod.getId(), id);
    }

    @Override
    public @NonNull List<TradeCurrency> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    @Override
    public @NonNull List<PaymentAccountFormField.FieldId> getInputFieldIds() {
        throw new RuntimeException("Not implemented");
    }

    public String getEmail() {
        return ((WesternUnionAccountPayload) paymentAccountPayload).getEmail();
    }

    public void setEmail(String email) {
        ((WesternUnionAccountPayload) paymentAccountPayload).setEmail(email);
    }

    public String getFullName() {
        return ((WesternUnionAccountPayload) paymentAccountPayload).getHolderName();
    }

    public void setFullName(String email) {
        ((WesternUnionAccountPayload) paymentAccountPayload).setHolderName(email);
    }

    public String getCity() {
        return ((WesternUnionAccountPayload) paymentAccountPayload).getCity();
    }

    public void setCity(String email) {
        ((WesternUnionAccountPayload) paymentAccountPayload).setCity(email);
    }

    public String getState() {
        return ((WesternUnionAccountPayload) paymentAccountPayload).getState();
    }

    public void setState(String email) {
        ((WesternUnionAccountPayload) paymentAccountPayload).setState(email);
    }
}
