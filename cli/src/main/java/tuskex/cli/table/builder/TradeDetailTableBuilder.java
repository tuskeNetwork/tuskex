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

package tuskex.cli.table.builder;

import tuskex.cli.table.Table;
import tuskex.cli.table.column.Column;
import tuskex.proto.grpc.TradeInfo;

import java.util.ArrayList;
import java.util.List;

import static tuskex.cli.table.builder.TableType.TRADE_DETAIL_TBL;

/**
 * Builds a {@code tuskex.cli.table.Table} from a {@code tuskex.proto.grpc.TradeInfo} object.
 */
@SuppressWarnings("ConstantConditions")
class TradeDetailTableBuilder extends AbstractTradeListBuilder {

    TradeDetailTableBuilder(List<?> protos) {
        super(TRADE_DETAIL_TBL, protos);
    }

    /**
     * Build a single row trade detail table.
     * @return Table containing one row
     */
    @Override
    public Table build() {
        // A trade detail table only has one row.
        var trade = trades.get(0);
        populateColumns(trade);
        List<Column<?>> columns = defineColumnList(trade);
        return new Table(columns.toArray(new Column<?>[0]));
    }

    private void populateColumns(TradeInfo trade) {
        populateTuskexV1TradeColumns(trade);
    }

    private void populateTuskexV1TradeColumns(TradeInfo trade) {
        colTradeId.addRow(trade.getShortId());
        colRole.addRow(trade.getRole());
        colPrice.addRow(trade.getPrice());
        colAmount.addRow(toTradeAmount.apply(trade));
        colTuskexTradeFee.addRow(toMyMakerOrTakerFee.apply(trade));
        colIsDepositPublished.addRow(trade.getIsDepositsPublished());
        colIsDepositConfirmed.addRow(trade.getIsDepositsUnlocked());
        colTradeCost.addRow(toTradeVolumeAsString.apply(trade));
        colIsPaymentSentMessageSent.addRow(trade.getIsPaymentSent());
        colIsPaymentReceivedMessageSent.addRow(trade.getIsPaymentReceived());
        colIsPayoutPublished.addRow(trade.getIsPayoutPublished());
        colIsCompleted.addRow(trade.getIsCompleted());
        if (colCryptoReceiveAddressColumn != null)
            colCryptoReceiveAddressColumn.addRow(toCryptoReceiveAddress.apply(trade));
    }

    private List<Column<?>> defineColumnList(TradeInfo trade) {
        return getTuskexV1TradeColumnList();
    }

    private List<Column<?>> getTuskexV1TradeColumnList() {
        List<Column<?>> columns = new ArrayList<>() {{
            add(colTradeId);
            add(colRole);
            add(colPrice.justify());
            add(colAmount.asStringColumn());
            add(colTuskexTradeFee.asStringColumn());
            add(colIsDepositPublished.asStringColumn());
            add(colIsDepositConfirmed.asStringColumn());
            add(colTradeCost.justify());
            add(colIsPaymentSentMessageSent.asStringColumn());
            add(colIsPaymentReceivedMessageSent.asStringColumn());
            add(colIsPayoutPublished.asStringColumn());
            add(colIsCompleted.asStringColumn());
        }};

        if (colCryptoReceiveAddressColumn != null)
            columns.add(colCryptoReceiveAddressColumn);

        return columns;
    }
}
