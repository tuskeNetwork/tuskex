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

package tuskex.desktop.main.offer;

import tuskex.common.UserThread;
import tuskex.core.offer.OfferUtil;
import tuskex.core.tsk.model.TskAddressEntry;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.common.model.ActivatableDataModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;

import java.math.BigInteger;

/**
 * Domain for that UI element.
 * Note that the create offer domain has a deeper scope in the application domain
 * (TradeManager).  That model is just responsible for the domain specific parts displayed
 * needed in that UI element.
 */
public abstract class OfferDataModel extends ActivatableDataModel {
    @Getter
    protected final TskWalletService tskWalletService;
    protected final OfferUtil offerUtil;

    @Getter
    protected final BooleanProperty isTskWalletFunded = new SimpleBooleanProperty();
    @Getter
    protected final ObjectProperty<BigInteger> totalToPay = new SimpleObjectProperty<>();
    @Getter
    protected final ObjectProperty<BigInteger> balance = new SimpleObjectProperty<>();
    @Getter
    protected final ObjectProperty<BigInteger> availableBalance = new SimpleObjectProperty<>();
    @Getter
    protected final ObjectProperty<BigInteger> missingCoin = new SimpleObjectProperty<>(BigInteger.ZERO);
    @Getter
    protected final BooleanProperty showWalletFundedNotification = new SimpleBooleanProperty();
    @Getter
    protected BigInteger totalBalance;
    @Getter
    protected BigInteger totalAvailableBalance;
    protected TskAddressEntry addressEntry;
    protected boolean useSavingsWallet;

    public OfferDataModel(TskWalletService tskWalletService, OfferUtil offerUtil) {
        this.tskWalletService = tskWalletService;
        this.offerUtil = offerUtil;
    }

    protected void updateBalance() {
        updateBalances();
        UserThread.await(() -> {
            missingCoin.set(offerUtil.getBalanceShortage(totalToPay.get(), balance.get()));
            isTskWalletFunded.set(offerUtil.isBalanceSufficient(totalToPay.get(), balance.get()));
            if (totalToPay.get() != null && isTskWalletFunded.get() && !showWalletFundedNotification.get()) {
                showWalletFundedNotification.set(true);
            }
        });
    }

    protected void updateAvailableBalance() {
        updateBalances();
        UserThread.await(() -> {
            missingCoin.set(offerUtil.getBalanceShortage(totalToPay.get(), availableBalance.get()));
            isTskWalletFunded.set(offerUtil.isBalanceSufficient(totalToPay.get(), availableBalance.get()));
            if (totalToPay.get() != null && isTskWalletFunded.get() && !showWalletFundedNotification.get()) {
                showWalletFundedNotification.set(true);
            }
        });
    }

    private void updateBalances() {
        BigInteger tradeWalletBalance = tskWalletService.getBalanceForSubaddress(addressEntry.getSubaddressIndex());
        BigInteger tradeWalletAvailableBalance = tskWalletService.getAvailableBalanceForSubaddress(addressEntry.getSubaddressIndex());
        BigInteger walletBalance = tskWalletService.getBalance();
        BigInteger walletAvailableBalance = tskWalletService.getAvailableBalance();
        UserThread.await(() -> {
            if (useSavingsWallet) {
                totalBalance = walletBalance;
                totalAvailableBalance = walletAvailableBalance;
                if (totalToPay.get() != null) {
                    balance.set(totalToPay.get().min(totalBalance));
                    availableBalance.set(totalToPay.get().min(totalAvailableBalance));
                }
            } else {
                balance.set(tradeWalletBalance);
                availableBalance.set(tradeWalletAvailableBalance);
            }
        });

    }
}
