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

package tuskex.desktop.components.paymentmethods;

import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.locale.Res;
import tuskex.core.payment.CashAppAccount;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.payment.payload.CashAppAccountPayload;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.validation.EmailOrMobileNrOrCashtagValidator;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.validation.InputValidator;
import tuskex.desktop.components.InputTextField;
import tuskex.desktop.util.FormBuilder;
import tuskex.desktop.util.Layout;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

import static tuskex.desktop.util.FormBuilder.addCompactTopLabelTextField;
import static tuskex.desktop.util.FormBuilder.addCompactTopLabelTextFieldWithCopyIcon;
import static tuskex.desktop.util.FormBuilder.addTopLabelFlowPane;

public class CashAppForm extends PaymentMethodForm {
    private final CashAppAccount cashAppAccount;
    private final EmailOrMobileNrOrCashtagValidator cashAppValidator;

    public static int addFormForBuyer(GridPane gridPane, int gridRow, PaymentAccountPayload paymentAccountPayload) {
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, Res.get("payment.email.mobile.cashtag"), ((CashAppAccountPayload) paymentAccountPayload).getEmailOrMobileNrOrCashtag());
        return gridRow;
    }

    public CashAppForm(PaymentAccount paymentAccount, AccountAgeWitnessService accountAgeWitnessService,
            EmailOrMobileNrOrCashtagValidator cashAppValidator, InputValidator inputValidator, GridPane gridPane,
            int gridRow,
            CoinFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.cashAppAccount = (CashAppAccount) paymentAccount;
        this.cashAppValidator = cashAppValidator;
    }

    @Override
    public void addFormForAddAccount() {
        gridRowFrom = gridRow + 1;

        InputTextField mobileNrInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow,
                Res.get("payment.email.mobile.cashtag"));
        mobileNrInputTextField.setValidator(cashAppValidator);
        mobileNrInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            cashAppAccount.setEmailOrMobileNrOrCashtag(newValue.trim());
            updateFromInputs();
        });
        addCurrenciesGrid(true);
        addLimitations(false);
        addAccountNameTextFieldWithAutoFillToggleButton();
    }

    private void addCurrenciesGrid(boolean isEditable) {
        FlowPane flowPane = addTopLabelFlowPane(gridPane, ++gridRow,
                Res.get("payment.supportedCurrencies"), Layout.FLOATING_LABEL_DISTANCE * 3,
                Layout.FLOATING_LABEL_DISTANCE * 3).second;

        if (isEditable)
            flowPane.setId("flow-pane-checkboxes-bg");
        else
            flowPane.setId("flow-pane-checkboxes-non-editable-bg");

        cashAppAccount.getSupportedCurrencies().forEach(e ->
                fillUpFlowPaneWithCurrencies(isEditable, flowPane, e, cashAppAccount));
    }

    @Override
    protected void autoFillNameTextField() {
        setAccountNameWithString(cashAppAccount.getEmailOrMobileNrOrCashtag());
    }

    @Override
    public void addFormForEditAccount() {
        gridRowFrom = gridRow;
        addAccountNameTextFieldWithAutoFillToggleButton();
        TextField field = addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("payment.email.mobile.cashtag"),
                cashAppAccount.getEmailOrMobileNrOrCashtag()).second;
        field.setMouseTransparent(false);
        addLimitations(true);
        addCurrenciesGrid(false);
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && cashAppValidator.validate(cashAppAccount.getEmailOrMobileNrOrCashtag()).isValid
                && cashAppAccount.getTradeCurrencies().size() > 0);
    }
}
