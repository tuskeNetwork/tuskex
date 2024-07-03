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

package tuskex.desktop.components.paymentmethods;

import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.locale.CountryUtil;
import tuskex.core.locale.Res;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.payment.TikkieAccount;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.payload.TikkieAccountPayload;
import tuskex.core.payment.validation.IBANValidator;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.validation.InputValidator;
import tuskex.desktop.components.InputTextField;
import tuskex.desktop.util.FormBuilder;
import javafx.scene.layout.GridPane;

import static tuskex.desktop.util.FormBuilder.addCompactTopLabelTextField;
import static tuskex.desktop.util.FormBuilder.addCompactTopLabelTextFieldWithCopyIcon;
import static tuskex.desktop.util.FormBuilder.addTopLabelTextField;

public class TikkieForm extends PaymentMethodForm {
    private final TikkieAccount account;
    private final IBANValidator ibanValidator = new IBANValidator("NL");

    public static int addFormForBuyer(GridPane gridPane, int gridRow,
                                      PaymentAccountPayload paymentAccountPayload) {
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, 0, Res.get("payment.tikkie.iban"),
                ((TikkieAccountPayload) paymentAccountPayload).getIban());
        return gridRow;
    }

    public TikkieForm(PaymentAccount paymentAccount, AccountAgeWitnessService accountAgeWitnessService,
                        InputValidator inputValidator, GridPane gridPane,
                        int gridRow, CoinFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.account = (TikkieAccount) paymentAccount;
    }

    @Override
    public void addFormForAddAccount() {
        // this payment method is only for Netherlands/EUR
        CountryUtil.findCountryByCode("NL").ifPresent(account::setCountry);

        gridRowFrom = gridRow + 1;

        InputTextField ibanField = FormBuilder.addInputTextField(gridPane, ++gridRow, Res.get("payment.tikkie.iban"));
        ibanField.setValidator(ibanValidator);
        ibanField.textProperty().addListener((ov, oldValue, newValue) -> {
            account.setIban(newValue.trim());
            updateFromInputs();
        });

        addTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"), account.getSingleTradeCurrency().getNameAndCode());
        addTopLabelTextField(gridPane, ++gridRow, Res.get("shared.country"), account.getCountry().name);
        addLimitations(false);
        addAccountNameTextFieldWithAutoFillToggleButton();
    }

    @Override
    protected void autoFillNameTextField() {
        setAccountNameWithString(account.getIban());
    }

    @Override
    public void addFormForEditAccount() {
        gridRowFrom = gridRow;
        addAccountNameTextFieldWithAutoFillToggleButton();
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.paymentMethod"),
                Res.get(account.getPaymentMethod().getId()));
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("payment.tikkie.iban"), account.getIban())
                .second.setMouseTransparent(false);
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"), account.getSingleTradeCurrency().getNameAndCode());
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.country"), account.getCountry().name);
        addLimitations(true);
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && ibanValidator.validate(account.getIban()).isValid);
    }
}
