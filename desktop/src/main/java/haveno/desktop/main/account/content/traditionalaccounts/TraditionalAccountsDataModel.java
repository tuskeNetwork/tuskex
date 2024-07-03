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

package tuskex.desktop.main.account.content.traditionalaccounts;

import com.google.inject.Inject;
import tuskex.common.crypto.KeyRing;
import tuskex.common.file.CorruptedStorageFileHandler;
import tuskex.common.proto.persistable.PersistenceProtoResolver;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.locale.CryptoCurrency;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.TraditionalCurrency;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.payment.AssetAccount;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.trade.TradeManager;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.desktop.common.model.ActivatableDataModel;
import tuskex.desktop.util.GUIUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class TraditionalAccountsDataModel extends ActivatableDataModel {

    private final User user;
    private final Preferences preferences;
    private final OpenOfferManager openOfferManager;
    private final TradeManager tradeManager;
    private final AccountAgeWitnessService accountAgeWitnessService;
    final ObservableList<PaymentAccount> paymentAccounts = FXCollections.observableArrayList();
    private final SetChangeListener<PaymentAccount> setChangeListener;
    private final String accountsFileName = "FiatPaymentAccounts";
    private final PersistenceProtoResolver persistenceProtoResolver;
    private final CorruptedStorageFileHandler corruptedStorageFileHandler;
    private final KeyRing keyRing;

    @Inject
    public TraditionalAccountsDataModel(User user,
                                 Preferences preferences,
                                 OpenOfferManager openOfferManager,
                                 TradeManager tradeManager,
                                 AccountAgeWitnessService accountAgeWitnessService,
                                 PersistenceProtoResolver persistenceProtoResolver,
                                 CorruptedStorageFileHandler corruptedStorageFileHandler,
                                 KeyRing keyRing) {
        this.user = user;
        this.preferences = preferences;
        this.openOfferManager = openOfferManager;
        this.tradeManager = tradeManager;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.persistenceProtoResolver = persistenceProtoResolver;
        this.corruptedStorageFileHandler = corruptedStorageFileHandler;
        this.keyRing = keyRing;
        setChangeListener = change -> fillAndSortPaymentAccounts();
    }

    @Override
    protected void activate() {
        user.getPaymentAccountsAsObservable().addListener(setChangeListener);
        fillAndSortPaymentAccounts();
    }

    private void fillAndSortPaymentAccounts() {
        if (user.getPaymentAccounts() != null) {
            List<PaymentAccount> list = user.getPaymentAccounts().stream()
                    .filter(paymentAccount -> !paymentAccount.getPaymentMethod().isBlockchain())
                    .collect(Collectors.toList());
            paymentAccounts.setAll(list);
            paymentAccounts.sort(Comparator.comparing(PaymentAccount::getAccountName));
        }
    }

    @Override
    protected void deactivate() {
        user.getPaymentAccountsAsObservable().removeListener(setChangeListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI actions
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void onSaveNewAccount(PaymentAccount paymentAccount) {
        TradeCurrency singleTradeCurrency = paymentAccount.getSingleTradeCurrency();
        List<TradeCurrency> tradeCurrencies = paymentAccount.getTradeCurrencies();
        if (singleTradeCurrency != null) {
            if (singleTradeCurrency instanceof TraditionalCurrency)
                preferences.addTraditionalCurrency((TraditionalCurrency) singleTradeCurrency);
            else
                preferences.addCryptoCurrency((CryptoCurrency) singleTradeCurrency);
        } else if (tradeCurrencies != null && !tradeCurrencies.isEmpty()) {
            if (tradeCurrencies.contains(CurrencyUtil.getDefaultTradeCurrency()))
                paymentAccount.setSelectedTradeCurrency(CurrencyUtil.getDefaultTradeCurrency());
            else
                paymentAccount.setSelectedTradeCurrency(tradeCurrencies.get(0));

            tradeCurrencies.forEach(tradeCurrency -> {
                if (tradeCurrency instanceof TraditionalCurrency)
                    preferences.addTraditionalCurrency((TraditionalCurrency) tradeCurrency);
                else
                    preferences.addCryptoCurrency((CryptoCurrency) tradeCurrency);
            });
        }

        user.addPaymentAccount(paymentAccount);
        paymentAccount.onPersistChanges();

        accountAgeWitnessService.publishMyAccountAgeWitness(paymentAccount.getPaymentAccountPayload());
        accountAgeWitnessService.signAndPublishSameNameAccounts();
    }

    public void onUpdateAccount(PaymentAccount paymentAccount) {
        paymentAccount.onPersistChanges();
        user.requestPersistence();
    }

    public boolean onDeleteAccount(PaymentAccount paymentAccount) {
        boolean isPaymentAccountUsed = openOfferManager.getObservableList().stream()
                .anyMatch(o -> o.getOffer().getMakerPaymentAccountId().equals(paymentAccount.getId()));
        isPaymentAccountUsed = isPaymentAccountUsed || tradeManager.getObservableList().stream()
                .anyMatch(t -> t.getOffer().getMakerPaymentAccountId().equals(paymentAccount.getId()) ||
                        paymentAccount.getId().equals(t.getTaker().getPaymentAccountId()));
        if (!isPaymentAccountUsed)
            user.removePaymentAccount(paymentAccount);
        return isPaymentAccountUsed;
    }

    public void onSelectAccount(PaymentAccount paymentAccount) {
        user.setCurrentPaymentAccount(paymentAccount);
    }

    public void exportAccounts(Stage stage) {
        if (user.getPaymentAccounts() != null) {
            ArrayList<PaymentAccount> accounts = new ArrayList<>(user.getPaymentAccounts().stream()
                    .filter(paymentAccount -> !(paymentAccount instanceof AssetAccount))
                    .collect(Collectors.toList()));
            GUIUtil.exportAccounts(accounts, accountsFileName, preferences, stage, persistenceProtoResolver, corruptedStorageFileHandler, keyRing);
        }
    }

    public void importAccounts(Stage stage) {
        GUIUtil.importAccounts(user, accountsFileName, preferences, stage, persistenceProtoResolver, corruptedStorageFileHandler, keyRing);
    }

    public int getNumPaymentAccounts() {
        return user.getPaymentAccounts() != null ? user.getPaymentAccounts().size() : 0;
    }
}
