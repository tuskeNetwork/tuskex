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

import com.jfoenix.controls.JFXTextArea;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.Res;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.payment.CashAtAtmAccount;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.payment.payload.CashAtAtmAccountPayload;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.validation.InputValidator;
import javafx.collections.FXCollections;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

import static tuskex.desktop.util.FormBuilder.addCompactTopLabelTextArea;
import static tuskex.desktop.util.FormBuilder.addCompactTopLabelTextField;
import static tuskex.desktop.util.FormBuilder.addTopLabelTextArea;

public class CashAtAtmForm extends PaymentMethodForm {
    private final CashAtAtmAccount cashAtAtmAccount;

    public static int addFormForBuyer(GridPane gridPane, int gridRow,
                                      PaymentAccountPayload paymentAccountPayload) {
        CashAtAtmAccountPayload cbm = (CashAtAtmAccountPayload) paymentAccountPayload;

        TextArea textExtraInfo = addCompactTopLabelTextArea(gridPane, gridRow, 1, Res.get("payment.shared.extraInfo"), "").second;
        textExtraInfo.setMinHeight(70);
        textExtraInfo.setEditable(false);
        textExtraInfo.setText(cbm.getExtraInfo());
        return gridRow;
    }

    public CashAtAtmForm(PaymentAccount paymentAccount,
                                  AccountAgeWitnessService accountAgeWitnessService,
                                  InputValidator inputValidator, GridPane gridPane, int gridRow, CoinFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.cashAtAtmAccount = (CashAtAtmAccount) paymentAccount;
    }

    @Override
    public void addFormForAddAccount() {
        gridRowFrom = gridRow + 1;

        addTradeCurrencyComboBox();
        currencyComboBox.setItems(FXCollections.observableArrayList(CurrencyUtil.getAllSortedFiatCurrencies()));

        TextArea extraTextArea = addTopLabelTextArea(gridPane, ++gridRow,
                Res.get("payment.shared.optionalExtra"), Res.get("payment.cashAtAtm.extraInfo.prompt")).second;
        extraTextArea.setMinHeight(70);
        ((JFXTextArea) extraTextArea).setLabelFloat(false);
        extraTextArea.textProperty().addListener((ov, oldValue, newValue) -> {
            cashAtAtmAccount.setExtraInfo(newValue);
            updateFromInputs();
        });

        addLimitations(false);
        addAccountNameTextFieldWithAutoFillToggleButton();
    }

    @Override
    protected void autoFillNameTextField() {
        setAccountNameWithString(cashAtAtmAccount.getExtraInfo().substring(0, Math.min(50, cashAtAtmAccount.getExtraInfo().length())));
    }

    @Override
    public void addFormForEditAccount() {
        gridRowFrom = gridRow;
        addAccountNameTextFieldWithAutoFillToggleButton();
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.paymentMethod"),
                Res.get(cashAtAtmAccount.getPaymentMethod().getId()));

        TradeCurrency tradeCurrency = paymentAccount.getSingleTradeCurrency();
        String nameAndCode = tradeCurrency != null ? tradeCurrency.getNameAndCode() : "";
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"), nameAndCode);

        TextArea textAreaExtra = addCompactTopLabelTextArea(gridPane, ++gridRow, Res.get("payment.shared.extraInfo"), "").second;
        textAreaExtra.setText(cashAtAtmAccount.getExtraInfo());
        textAreaExtra.setMinHeight(70);
        textAreaExtra.setEditable(false);

        addLimitations(true);
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && !cashAtAtmAccount.getExtraInfo().isEmpty()
                && paymentAccount.getSingleTradeCurrency() != null);
    }
}
