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

package tuskex.desktop.main.funds.transactions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import tuskex.core.user.Preferences;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.tsk.wallet.TskWalletService;
import javax.annotation.Nullable;
import monero.wallet.model.MoneroTxWallet;


@Singleton
public class TransactionListItemFactory {
    private final TskWalletService tskWalletService;
    private final CoinFormatter formatter;
    private final Preferences preferences;

    @Inject
    TransactionListItemFactory(TskWalletService tskWalletService,
                               @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter formatter,
                               Preferences preferences) {
        this.tskWalletService = tskWalletService;
        this.formatter = formatter;
        this.preferences = preferences;
    }

    TransactionsListItem create(MoneroTxWallet transaction, @Nullable TransactionAwareTradable tradable) {
        return new TransactionsListItem(transaction,
                tskWalletService,
                tradable);
    }
}
