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

package tuskex.core.tsk;

import com.google.inject.Inject;
import tuskex.common.UserThread;
import tuskex.core.api.model.TskBalanceInfo;
import tuskex.core.offer.OpenOffer;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.support.dispute.Dispute;
import tuskex.core.support.dispute.refund.RefundManager;
import tuskex.core.trade.ClosedTradableManager;
import tuskex.core.trade.MakerTrade;
import tuskex.core.trade.Trade;
import tuskex.core.trade.TradeManager;
import tuskex.core.trade.failed.FailedTradesManager;
import tuskex.core.tsk.listeners.TskBalanceListener;
import tuskex.core.tsk.wallet.TskWalletService;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import monero.wallet.model.MoneroOutputQuery;
import monero.wallet.model.MoneroOutputWallet;

@Slf4j
public class Balances {
    private final TradeManager tradeManager;
    private final TskWalletService tskWalletService;
    private final OpenOfferManager openOfferManager;
    private final RefundManager refundManager;

    @Getter
    private BigInteger availableBalance;
    @Getter
    private BigInteger pendingBalance;
    @Getter
    private BigInteger reservedOfferBalance;
    @Getter
    private BigInteger reservedTradeBalance;
    @Getter
    private BigInteger reservedBalance; // TODO (woodser): this balance is sum of reserved funds for offers and trade multisigs; remove?

    @Getter
    private final IntegerProperty updateCounter = new SimpleIntegerProperty(0);

    @Inject
    public Balances(TradeManager tradeManager,
                    TskWalletService tskWalletService,
                    OpenOfferManager openOfferManager,
                    ClosedTradableManager closedTradableManager,
                    FailedTradesManager failedTradesManager,
                    RefundManager refundManager) {
        this.tradeManager = tradeManager;
        this.tskWalletService = tskWalletService;
        this.openOfferManager = openOfferManager;
        this.refundManager = refundManager;
    }

    public void onAllServicesInitialized() {
        openOfferManager.getObservableList().addListener((ListChangeListener<OpenOffer>) c -> updateBalances());
        tradeManager.getObservableList().addListener((ListChangeListener<Trade>) change -> updateBalances());
        refundManager.getDisputesAsObservableList().addListener((ListChangeListener<Dispute>) c -> updateBalances());
        tskWalletService.addBalanceListener(new TskBalanceListener() {
            @Override
            public void onBalanceChanged(BigInteger balance) {
                updateBalances();
            }
        });
        updateBalances();
    }

    public TskBalanceInfo getBalances() {
        synchronized (this) {
            return new TskBalanceInfo(availableBalance.longValue() + pendingBalance.longValue(),
                availableBalance.longValue(),
                pendingBalance.longValue(),
                reservedOfferBalance.longValue(),
                reservedTradeBalance.longValue());
        }
    }

    private void updateBalances() {
        synchronized (this) {
            
            // get wallet balances
            BigInteger balance = tskWalletService.getWallet() == null ? BigInteger.ZERO : tskWalletService.getBalance();
            availableBalance = tskWalletService.getWallet() == null ? BigInteger.ZERO : tskWalletService.getAvailableBalance();

            // calculate pending balance by adding frozen trade balances - reserved amounts
            pendingBalance = balance.subtract(availableBalance);
            List<Trade> trades = tradeManager.getTradesStreamWithFundsLockedIn().collect(Collectors.toList());
            for (Trade trade : trades) {
                if (trade.getFrozenAmount().equals(new BigInteger("0"))) continue;
                BigInteger tradeFee = trade instanceof MakerTrade ? trade.getMakerFee() : trade.getTakerFee();
                pendingBalance = pendingBalance.add(trade.getFrozenAmount()).subtract(trade.getReservedAmount()).subtract(tradeFee).subtract(trade.getSelf().getDepositTxFee());
            }

            // calculate reserved offer balance
            reservedOfferBalance = BigInteger.ZERO;
            if (tskWalletService.getWallet() != null) {
                List<MoneroOutputWallet> frozenOutputs = tskWalletService.getOutputs(new MoneroOutputQuery().setIsFrozen(true).setIsSpent(false));
                for (MoneroOutputWallet frozenOutput : frozenOutputs) reservedOfferBalance = reservedOfferBalance.add(frozenOutput.getAmount());
            }
            for (Trade trade : trades) {
                reservedOfferBalance = reservedOfferBalance.subtract(trade.getFrozenAmount()); // subtract frozen trade balances
            }

            // calculate reserved trade balance
            reservedTradeBalance = BigInteger.ZERO;
            for (Trade trade : trades) {
                reservedTradeBalance = reservedTradeBalance.add(trade.getReservedAmount());
            }

            // calculate reserved balance
            reservedBalance = reservedOfferBalance.add(reservedTradeBalance);

            // notify balance update
            UserThread.execute(() -> updateCounter.set(updateCounter.get() + 1));
        }
    }
}
