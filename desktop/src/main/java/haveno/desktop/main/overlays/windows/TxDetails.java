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

package tuskex.desktop.main.overlays.windows;

import tuskex.core.locale.Res;
import tuskex.desktop.components.TxIdTextField;
import tuskex.desktop.main.overlays.Overlay;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import static tuskex.desktop.util.FormBuilder.addConfirmationLabelLabel;
import static tuskex.desktop.util.FormBuilder.addConfirmationLabelTextFieldWithCopyIcon;
import static tuskex.desktop.util.FormBuilder.addLabelTxIdTextField;
import static tuskex.desktop.util.FormBuilder.addMultilineLabel;

public class TxDetails extends Overlay<TxDetails> {

    protected String txId, address, amount, fee, memo;
    protected TxIdTextField txIdTextField;

    public TxDetails(String txId, String address, String amount, String fee, String memo) {
        type = Type.Attention;
        this.txId = txId;
        this.address = address;
        this.amount = amount;
        this.fee = fee;
        this.memo = memo;
    }

    public void show() {
        rowIndex = -1;
        width = 918;
        if (headLine == null)
            headLine = Res.get("txDetailsWindow.headline");
        createGridPane();
        gridPane.setHgap(15);
        addHeadLine();
        addContent();
        addButtons();
        addDontShowAgainCheckBox();
        applyStyles();
        display();
    }

    protected void addContent() {
        GridPane.setColumnSpan(
                addMultilineLabel(gridPane, ++rowIndex, Res.get("txDetailsWindow.tsk.note"), 0), 2);
        Region spacer = new Region();
        spacer.setMinHeight(20);
        gridPane.add(spacer, 0, ++rowIndex);
        addConfirmationLabelTextFieldWithCopyIcon(gridPane, ++rowIndex, Res.get("txDetailsWindow.sentTo"), address);
        addConfirmationLabelLabel(gridPane, ++rowIndex, Res.get("shared.amount"), amount);
        addConfirmationLabelLabel(gridPane, ++rowIndex, Res.get("shared.txFee"), fee);
        if (memo != null && !"".equals(memo)) addConfirmationLabelLabel(gridPane, ++rowIndex, Res.get("funds.withdrawal.memoLabel"), memo);
        txIdTextField = addLabelTxIdTextField(gridPane, ++rowIndex, Res.get("txDetailsWindow.txId"), txId).second;
    }
}
