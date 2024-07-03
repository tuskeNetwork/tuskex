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
import tuskex.proto.grpc.AddressBalanceInfo;

import java.util.List;
import java.util.stream.Collectors;

import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_ADDRESS;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_AVAILABLE_BALANCE;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_CONFIRMATIONS;
import static tuskex.cli.table.builder.TableBuilderConstants.COL_HEADER_IS_USED_ADDRESS;
import static tuskex.cli.table.builder.TableType.ADDRESS_BALANCE_TBL;
import static java.lang.String.format;

/**
 * Builds a {@code tuskex.cli.table.Table} from a List of
 * {@code tuskex.proto.grpc.AddressBalanceInfo} objects.
 */
class AddressBalanceTableBuilder extends AbstractTableBuilder {

    // Default columns not dynamically generated with address info.
    private final Column<String> colAddress;
    private final Column<Long> colAvailableBalance;
    private final Column<Long> colConfirmations;
    private final Column<Boolean> colIsUsed;

    AddressBalanceTableBuilder(List<?> protos) {
        super(ADDRESS_BALANCE_TBL, protos);
        colAddress = new StringColumn(format(COL_HEADER_ADDRESS, "BTC"));
        this.colAvailableBalance = new SatoshiColumn(COL_HEADER_AVAILABLE_BALANCE);
        this.colConfirmations = new LongColumn(COL_HEADER_CONFIRMATIONS);
        this.colIsUsed = new BooleanColumn(COL_HEADER_IS_USED_ADDRESS);
    }

    public Table build() {
        List<AddressBalanceInfo> addresses = protos.stream()
                .map(a -> (AddressBalanceInfo) a)
                .collect(Collectors.toList());

        // Populate columns with address info.
        //noinspection SimplifyStreamApiCallChains
        addresses.stream().forEachOrdered(a -> {
            colAddress.addRow(a.getAddress());
            colAvailableBalance.addRow(a.getBalance());
            colConfirmations.addRow(a.getNumConfirmations());
            colIsUsed.addRow(!a.getIsAddressUnused());
        });

        // Define and return the table instance with populated columns.
        return new Table(colAddress,
                colAvailableBalance.asStringColumn(),
                colConfirmations.asStringColumn(),
                colIsUsed.asStringColumn());
    }
}
