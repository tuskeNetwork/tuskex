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
import tuskex.cli.table.column.BooleanColumn;
import tuskex.cli.table.column.Column;
import tuskex.cli.table.column.LongColumn;
import tuskex.cli.table.column.SatoshiColumn;
import tuskex.cli.table.column.StringColumn;

import javax.annotation.Nullable;
import java.util.List;

import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_TX_FEE;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_TX_ID;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_TX_INPUT_SUM;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_TX_IS_CONFIRMED;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_TX_OUTPUT_SUM;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_TX_SIZE;
import static tuskex.cli.table.builder.TableType.TRANSACTION_TBL;

/**
 * Builds a {@code tuskex.cli.table.Table} from a {@code tuskex.proto.grpc.TxInfo} object.
 */
class TransactionTableBuilder extends AbstractTableBuilder {

    // Default columns not dynamically generated with tx info.
    private final Column<String> colTxId;
    private final Column<Boolean> colIsConfirmed;
    private final Column<Long> colInputSum;
    private final Column<Long> colOutputSum;
    private final Column<Long> colTxFee;
    private final Column<Long> colTxSize;

    TransactionTableBuilder(List<?> protos) {
        super(TRANSACTION_TBL, protos);
        this.colTxId = new StringColumn(COL_HEADER_TX_ID);
        this.colIsConfirmed = new BooleanColumn(COL_HEADER_TX_IS_CONFIRMED);
        this.colInputSum = new SatoshiColumn(COL_HEADER_TX_INPUT_SUM);
        this.colOutputSum = new SatoshiColumn(COL_HEADER_TX_OUTPUT_SUM);
        this.colTxFee = new SatoshiColumn(COL_HEADER_TX_FEE);
        this.colTxSize = new LongColumn(COL_HEADER_TX_SIZE);
    }

    public Table build() {
        // TODO Add 'gettransactions' api method & show multiple tx in the console.
        //  For now, a tx tbl is only one row.

        // Declare the columns derived from tx info.

        @Nullable
        Column<String> colMemo = null;

        // Populate columns with tx info.

        colTxId.addRow(null);
        colIsConfirmed.addRow(null);
        colInputSum.addRow(null);
        colOutputSum.addRow(null);
        colTxFee.addRow(null);
        colTxSize.addRow(null);
        if (colMemo != null)
            colMemo.addRow(null);

        // Define and return the table instance with populated columns.

        if (colMemo != null) {
            return new Table(colTxId,
                    colIsConfirmed.asStringColumn(),
                    colInputSum.asStringColumn(),
                    colOutputSum.asStringColumn(),
                    colTxFee.asStringColumn(),
                    colTxSize.asStringColumn(),
                    colMemo);
        } else {
            return new Table(colTxId,
                    colIsConfirmed.asStringColumn(),
                    colInputSum.asStringColumn(),
                    colOutputSum.asStringColumn(),
                    colTxFee.asStringColumn(),
                    colTxSize.asStringColumn());
        }
    }
}
