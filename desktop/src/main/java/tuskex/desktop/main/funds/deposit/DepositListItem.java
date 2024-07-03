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

package tuskex.desktop.main.funds.deposit;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import tuskex.core.locale.Res;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.components.indicator.TxConfidenceIndicator;
import tuskex.desktop.util.GUIUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Tooltip;
import lombok.extern.slf4j.Slf4j;
import monero.daemon.model.MoneroTx;
import monero.wallet.model.MoneroTxWallet;

import java.math.BigInteger;
import java.util.List;

@Slf4j
class DepositListItem {
    private final StringProperty balance = new SimpleStringProperty();
    private final TskAddressEntry addressEntry;
    private final TskWalletService tskWalletService;
    private BigInteger balanceAsBI;
    private String usage = "-";
    private int numTxsWithOutputs = 0;
    private final Supplier<LazyFields> lazyFieldsSupplier;

    private static class LazyFields {
        TxConfidenceIndicator txConfidenceIndicator;
        Tooltip tooltip;
    }

    private LazyFields lazy() {
        return lazyFieldsSupplier.get();
    }

    DepositListItem(TskAddressEntry addressEntry, TskWalletService tskWalletService, CoinFormatter formatter) {
        this.tskWalletService = tskWalletService;
        this.addressEntry = addressEntry;

        balanceAsBI = tskWalletService.getBalanceForSubaddress(addressEntry.getSubaddressIndex());
        balance.set(TuskexUtils.formatTsk(balanceAsBI));

        updateUsage(addressEntry.getSubaddressIndex());

        // confidence
        lazyFieldsSupplier = Suppliers.memoize(() -> new LazyFields() {{
            txConfidenceIndicator = new TxConfidenceIndicator();
            txConfidenceIndicator.setId("funds-confidence");
            tooltip = new Tooltip(Res.get("shared.notUsedYet"));
            txConfidenceIndicator.setProgress(0);
            txConfidenceIndicator.setTooltip(tooltip);
            MoneroTx tx = getTxWithFewestConfirmations();
            if (tx == null) {
                txConfidenceIndicator.setVisible(false);
            } else {
                GUIUtil.updateConfidence(tx, tooltip, txConfidenceIndicator);
                txConfidenceIndicator.setVisible(true);
            }
        }});
    }

    private void updateUsage(int subaddressIndex) {
        numTxsWithOutputs = tskWalletService.getNumTxsWithIncomingOutputs(addressEntry.getSubaddressIndex());
        switch (addressEntry.getContext()) {
            case BASE_ADDRESS:
                usage = Res.get("funds.deposit.baseAddress");
                break;
            case AVAILABLE:
                usage = numTxsWithOutputs == 0 ? Res.get("funds.deposit.unused") : Res.get("funds.deposit.usedInTx", numTxsWithOutputs);
                break;
            case OFFER_FUNDING:
                usage = Res.get("funds.deposit.offerFunding", addressEntry.getShortOfferId());
                break;
            case TRADE_PAYOUT:
                usage = Res.get("funds.deposit.tradePayout", addressEntry.getShortOfferId());
                break;
            default:
                usage = addressEntry.getContext().toString();
            }
    }

    public void cleanup() {
    }

    public TxConfidenceIndicator getTxConfidenceIndicator() {
        return lazy().txConfidenceIndicator;
    }

    public String getAddressString() {
        return addressEntry.getAddressString();
    }

    public int getSubaddressIndex() {
        return addressEntry.getSubaddressIndex();
    }

    public String getUsage() {
        return usage;
    }

    public final StringProperty balanceProperty() {
        return this.balance;
    }

    public String getBalance() {
        return balance.get();
    }

    public BigInteger getBalanceAsBI() {
        return balanceAsBI;
    }

    public int getNumTxsWithOutputs() {
        return numTxsWithOutputs;
    }

    public long getNumConfirmationsSinceFirstUsed() {
        MoneroTx tx = getTxWithFewestConfirmations();
        return tx == null ? 0 : tx.getNumConfirmations();
    }

    private MoneroTxWallet getTxWithFewestConfirmations() {

        // get txs with incoming outputs to subaddress index
        List<MoneroTxWallet> txs = tskWalletService.getTxsWithIncomingOutputs(addressEntry.getSubaddressIndex());
        
        // get tx with fewest confirmations
        MoneroTxWallet highestTx = null;
        for (MoneroTxWallet tx : txs) {
            if (highestTx == null || tx.getHeight() == null || (highestTx.getHeight() != null && tx.getHeight() > highestTx.getHeight())) {
                highestTx = tx;
            }
        }
        return highestTx;
    }
}
