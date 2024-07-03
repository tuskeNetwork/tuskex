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
import tuskex.core.locale.Res;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.payment.payload.PayByMailAccountPayload;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.payload.PaymentMethod;
import lombok.NonNull;

import java.util.List;

public final class PayByMailAccount extends PaymentAccount {

    public static final List<TradeCurrency> SUPPORTED_CURRENCIES = CurrencyUtil.getAllTraditionalCurrencies();

    public PayByMailAccount() {
        super(PaymentMethod.PAY_BY_MAIL);
    }

    private static final List<PaymentAccountFormField.FieldId> INPUT_FIELD_IDS = List.of(
        PaymentAccountFormField.FieldId.TRADE_CURRENCIES,
        PaymentAccountFormField.FieldId.CONTACT,
        PaymentAccountFormField.FieldId.POSTAL_ADDRESS,
        PaymentAccountFormField.FieldId.EXTRA_INFO,
        PaymentAccountFormField.FieldId.ACCOUNT_NAME,
        PaymentAccountFormField.FieldId.SALT
    );

    @Override
    protected PaymentAccountPayload createPayload() {
        return new PayByMailAccountPayload(paymentMethod.getId(), id);
    }

    @Override
    public @NonNull List<TradeCurrency> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    @Override
    public @NonNull List<PaymentAccountFormField.FieldId> getInputFieldIds() {
        return INPUT_FIELD_IDS;
    }

    public void setPostalAddress(String postalAddress) {
        ((PayByMailAccountPayload) paymentAccountPayload).setPostalAddress(postalAddress);
    }

    public String getPostalAddress() {
        return ((PayByMailAccountPayload) paymentAccountPayload).getPostalAddress();
    }

    public void setContact(String contact) {
        ((PayByMailAccountPayload) paymentAccountPayload).setContact(contact);
    }

    public String getContact() {
        return ((PayByMailAccountPayload) paymentAccountPayload).getContact();
    }

    public void setExtraInfo(String extraInfo) {
        ((PayByMailAccountPayload) paymentAccountPayload).setExtraInfo(extraInfo);
    }

    public String getExtraInfo() {
        return ((PayByMailAccountPayload) paymentAccountPayload).getExtraInfo();
    }

    @Override
    protected PaymentAccountFormField getEmptyFormField(PaymentAccountFormField.FieldId fieldId) {
        var field = super.getEmptyFormField(fieldId);
        if (field.getId() == PaymentAccountFormField.FieldId.TRADE_CURRENCIES) field.setComponent(PaymentAccountFormField.Component.SELECT_ONE);
        if (field.getId() == PaymentAccountFormField.FieldId.CONTACT) field.setLabel(Res.get("payment.payByMail.contact.prompt"));
        if (field.getId() == PaymentAccountFormField.FieldId.POSTAL_ADDRESS) field.setLabel(Res.get("payment.postal.address"));
        if (field.getId() == PaymentAccountFormField.FieldId.EXTRA_INFO) field.setLabel(Res.get("payment.shared.optionalExtra"));
        return field;
    }
}
