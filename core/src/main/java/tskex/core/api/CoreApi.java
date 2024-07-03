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

package tuskex.core.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import tuskex.common.app.Version;
import tuskex.common.config.Config;
import tuskex.common.crypto.IncorrectPasswordException;
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.common.handlers.FaultHandler;
import tuskex.common.handlers.ResultHandler;
import tuskex.core.api.model.AddressBalanceInfo;
import tuskex.core.api.model.BalancesInfo;
import tuskex.core.api.model.MarketDepthInfo;
import tuskex.core.api.model.MarketPriceInfo;
import tuskex.core.api.model.PaymentAccountForm;
import tuskex.core.api.model.PaymentAccountFormField;
import tuskex.core.app.AppStartupState;
import tuskex.core.monetary.Price;
import tuskex.core.offer.Offer;
import tuskex.core.offer.OfferDirection;
import tuskex.core.offer.OpenOffer;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.payment.payload.PaymentMethod;
import tuskex.core.support.dispute.Attachment;
import tuskex.core.support.dispute.Dispute;
import tuskex.core.support.dispute.DisputeResult;
import tuskex.core.support.messages.ChatMessage;
import tuskex.core.trade.Trade;
import tuskex.core.trade.statistics.TradeStatistics3;
import tuskex.core.trade.statistics.TradeStatisticsManager;
import tuskex.core.tsk.TskNodeSettings;
import tuskex.proto.grpc.NotificationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import monero.common.MoneroRpcConnection;
import monero.wallet.model.MoneroDestination;
import monero.wallet.model.MoneroTxWallet;
import org.bitcoinj.core.Transaction;

/**
 * Provides high level interface to functionality of core Tuskex features.
 * E.g. useful for different APIs to access data of different domains of Tuskex.
 */
@Singleton
@Slf4j
public class CoreApi {

    @Getter
    private final Config config;
    private final AppStartupState appStartupState;
    private final CoreAccountService coreAccountService;
    private final CoreDisputeAgentsService coreDisputeAgentsService;
    private final CoreDisputesService coreDisputeService;
    private final CoreHelpService coreHelpService;
    private final CoreOffersService coreOffersService;
    private final CorePaymentAccountsService paymentAccountsService;
    private final CorePriceService corePriceService;
    private final CoreTradesService coreTradesService;
    private final CoreWalletsService walletsService;
    private final TradeStatisticsManager tradeStatisticsManager;
    private final CoreNotificationService notificationService;
    private final TskConnectionService tskConnectionService;
    private final TskLocalNode tskLocalNode;

    @Inject
    public CoreApi(Config config,
                   AppStartupState appStartupState,
                   CoreAccountService coreAccountService,
                   CoreDisputeAgentsService coreDisputeAgentsService,
                   CoreDisputesService coreDisputeService,
                   CoreHelpService coreHelpService,
                   CoreOffersService coreOffersService,
                   CorePaymentAccountsService paymentAccountsService,
                   CorePriceService corePriceService,
                   CoreTradesService coreTradesService,
                   CoreWalletsService walletsService,
                   TradeStatisticsManager tradeStatisticsManager,
                   CoreNotificationService notificationService,
                   TskConnectionService tskConnectionService,
                   TskLocalNode tskLocalNode) {
        this.config = config;
        this.appStartupState = appStartupState;
        this.coreAccountService = coreAccountService;
        this.coreDisputeAgentsService = coreDisputeAgentsService;
        this.coreDisputeService = coreDisputeService;
        this.coreHelpService = coreHelpService;
        this.coreOffersService = coreOffersService;
        this.paymentAccountsService = paymentAccountsService;
        this.coreTradesService = coreTradesService;
        this.corePriceService = corePriceService;
        this.walletsService = walletsService;
        this.tradeStatisticsManager = tradeStatisticsManager;
        this.notificationService = notificationService;
        this.tskConnectionService = tskConnectionService;
        this.tskLocalNode = tskLocalNode;
    }

    @SuppressWarnings("SameReturnValue")
    public String getVersion() {
        return Version.VERSION;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Help
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String getMethodHelp(String methodName) {
        return coreHelpService.getMethodHelp(methodName);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Account Service
    ///////////////////////////////////////////////////////////////////////////////////////////

    public boolean accountExists() {
        return coreAccountService.accountExists();
    }

    public boolean isAccountOpen() {
        return coreAccountService.isAccountOpen();
    }

    public void createAccount(String password) {
        coreAccountService.createAccount(password);
    }

    public void openAccount(String password) throws IncorrectPasswordException {
        coreAccountService.openAccount(password);
    }

    public boolean isAppInitialized() {
        return appStartupState.isApplicationFullyInitialized();
    }

    public void changePassword(String oldPassword, String newPassword) {
        coreAccountService.changePassword(oldPassword, newPassword);
    }

    public void closeAccount() {
        coreAccountService.closeAccount();
    }

    public void deleteAccount(Runnable onShutdown) {
        coreAccountService.deleteAccount(onShutdown);
    }

    public void backupAccount(int bufferSize, Consumer<InputStream> consume, Consumer<Exception> error) {
        coreAccountService.backupAccount(bufferSize, consume, error);
    }

    public void restoreAccount(InputStream zipStream, int bufferSize, Runnable onShutdown) throws Exception {
        coreAccountService.restoreAccount(zipStream, bufferSize, onShutdown);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Monero Connections
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void addTskConnection(MoneroRpcConnection connection) {
        tskConnectionService.addConnection(connection);
    }

    public void removeTskConnection(String connectionUri) {
        tskConnectionService.removeConnection(connectionUri);
    }

    public MoneroRpcConnection getTskConnection() {
        return tskConnectionService.getConnection();
    }

    public List<MoneroRpcConnection> getTskConnections() {
        return tskConnectionService.getConnections();
    }

    public void setTskConnection(String connectionUri) {
        tskConnectionService.setConnection(connectionUri);
    }

    public void setTskConnection(MoneroRpcConnection connection) {
        tskConnectionService.setConnection(connection);
    }

    public MoneroRpcConnection checkTskConnection() {
        return tskConnectionService.checkConnection();
    }

    public List<MoneroRpcConnection> checkTskConnections() {
        return tskConnectionService.checkConnections();
    }

    public void startCheckingTskConnection(Long refreshPeriod) {
        tskConnectionService.startCheckingConnection(refreshPeriod);
    }

    public void stopCheckingTskConnection() {
        tskConnectionService.stopCheckingConnection();
    }

    public MoneroRpcConnection getBestAvailableTskConnection() {
        return tskConnectionService.getBestAvailableConnection();
    }

    public void setTskConnectionAutoSwitch(boolean autoSwitch) {
        tskConnectionService.setAutoSwitch(autoSwitch);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Monero node
    ///////////////////////////////////////////////////////////////////////////////////////////

    public boolean isTskNodeOnline() {
        return tskLocalNode.isDetected();
    }

    public TskNodeSettings getTskNodeSettings() {
        return tskLocalNode.getNodeSettings();
    }

    public void startTskNode(TskNodeSettings settings) throws IOException {
        tskLocalNode.startNode(settings);
    }

    public void stopTskNode() {
        tskLocalNode.stopNode();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Wallets
    ///////////////////////////////////////////////////////////////////////////////////////////

    public BalancesInfo getBalances(String currencyCode) {
        return walletsService.getBalances(currencyCode);
    }

    public String getTskSeed() {
        return walletsService.getTskSeed();
    }

    public String getTskPrimaryAddress() {
        return walletsService.getTskPrimaryAddress();
    }

    public String getTskNewSubaddress() {
        return walletsService.getTskNewSubaddress();
    }

    public List<MoneroTxWallet> getTskTxs() {
        return walletsService.getTskTxs();
    }

    public MoneroTxWallet createTskTx(List<MoneroDestination> destinations) {
        return walletsService.createTskTx(destinations);
    }

    public String relayTskTx(String metadata) {
        return walletsService.relayTskTx(metadata);
    }

    public long getAddressBalance(String addressString) {
        return walletsService.getAddressBalance(addressString);
    }

    public AddressBalanceInfo getAddressBalanceInfo(String addressString) {
        return walletsService.getAddressBalanceInfo(addressString);
    }

    public List<AddressBalanceInfo> getFundingAddresses() {
        return walletsService.getFundingAddresses();
    }

    public Transaction getTransaction(String txId) {
        return walletsService.getTransaction(txId);
    }

    public void setWalletPassword(String password, String newPassword) {
        walletsService.setWalletPassword(password, newPassword);
    }

    public void lockWallet() {
        walletsService.lockWallet();
    }

    public void unlockWallet(String password, long timeout) {
        walletsService.unlockWallet(password, timeout);
    }

    public void removeWalletPassword(String password) {
        walletsService.removeWalletPassword(password);
    }

    public List<TradeStatistics3> getTradeStatistics() {
        return new ArrayList<>(tradeStatisticsManager.getObservableTradeStatisticsSet());
    }

    public int getNumConfirmationsForMostRecentTransaction(String addressString) {
        return walletsService.getNumConfirmationsForMostRecentTransaction(addressString);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Notifications
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void addNotificationListener(NotificationListener listener) {
        notificationService.addListener(listener);
    }

    public void sendNotification(NotificationMessage notification) {
        notificationService.sendNotification(notification);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Disputes
    ///////////////////////////////////////////////////////////////////////////////////////////

    public List<Dispute> getDisputes() {
        return coreDisputeService.getDisputes();
    }

    public Dispute getDispute(String tradeId) {
        return coreDisputeService.getDispute(tradeId);
    }

    public void openDispute(String tradeId, ResultHandler resultHandler, FaultHandler faultHandler) {
        coreDisputeService.openDispute(tradeId, resultHandler, faultHandler);
    }

    public void resolveDispute(String tradeId, DisputeResult.Winner winner, DisputeResult.Reason reason,
                               String summaryNotes, long customPayoutAmount) {
        coreDisputeService.resolveDispute(tradeId, winner, reason, summaryNotes, customPayoutAmount);
    }

    public void sendDisputeChatMessage(String disputeId, String message, ArrayList<Attachment> attachments) {
        coreDisputeService.sendDisputeChatMessage(disputeId, message, attachments);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Dispute Agents
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void registerDisputeAgent(String disputeAgentType, String registrationKey, ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        coreDisputeAgentsService.registerDisputeAgent(disputeAgentType, registrationKey, resultHandler, errorMessageHandler);
    }

    public void unregisterDisputeAgent(String disputeAgentType, ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        coreDisputeAgentsService.unregisterDisputeAgent(disputeAgentType, resultHandler, errorMessageHandler);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Offers
    ///////////////////////////////////////////////////////////////////////////////////////////

    public Offer getOffer(String id) {
        return coreOffersService.getOffer(id);
    }

    public List<Offer> getOffers(String direction, String currencyCode) {
        return coreOffersService.getOffers(direction, currencyCode);
    }

    public List<OpenOffer> getMyOffers(String direction, String currencyCode) {
        return coreOffersService.getMyOffers(direction, currencyCode);
    }

    public OpenOffer getMyOffer(String id) {
        return coreOffersService.getMyOffer(id);
    }

    public void postOffer(String currencyCode,
                                   String directionAsString,
                                   String priceAsString,
                                   boolean useMarketBasedPrice,
                                   double marketPriceMargin,
                                   long amountAsLong,
                                   long minAmountAsLong,
                                   double buyerSecurityDeposit,
                                   String triggerPriceAsString,
                                   boolean reserveExactAmount,
                                   String paymentAccountId,
                                   Consumer<Offer> resultHandler,
                                   ErrorMessageHandler errorMessageHandler) {
        coreOffersService.postOffer(currencyCode,
                directionAsString,
                priceAsString,
                useMarketBasedPrice,
                marketPriceMargin,
                amountAsLong,
                minAmountAsLong,
                buyerSecurityDeposit,
                triggerPriceAsString,
                reserveExactAmount,
                paymentAccountId,
                resultHandler,
                errorMessageHandler);
    }

    public Offer editOffer(String offerId,
                           String currencyCode,
                           OfferDirection direction,
                           Price price,
                           boolean useMarketBasedPrice,
                           double marketPriceMargin,
                           BigInteger amount,
                           BigInteger minAmount,
                           double buyerSecurityDeposit,
                           PaymentAccount paymentAccount) {
        return coreOffersService.editOffer(offerId,
                currencyCode,
                direction,
                price,
                useMarketBasedPrice,
                marketPriceMargin,
                amount,
                minAmount,
                buyerSecurityDeposit,
                paymentAccount);
    }

    public void cancelOffer(String id, ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        coreOffersService.cancelOffer(id, resultHandler, errorMessageHandler);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PaymentAccounts
    ///////////////////////////////////////////////////////////////////////////////////////////

    public PaymentAccount createPaymentAccount(PaymentAccountForm form) {
        return paymentAccountsService.createPaymentAccount(form);
    }

    public Set<PaymentAccount> getPaymentAccounts() {
        return paymentAccountsService.getPaymentAccounts();
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentAccountsService.getPaymentMethods();
    }

    public PaymentAccountForm getPaymentAccountForm(String paymentMethodId) {
        return paymentAccountsService.getPaymentAccountForm(paymentMethodId);
    }

    public PaymentAccountForm getPaymentAccountForm(PaymentAccount paymentAccount) {
        return paymentAccountsService.getPaymentAccountForm(paymentAccount);
    }

    public PaymentAccount createCryptoCurrencyPaymentAccount(String accountName,
                                                             String currencyCode,
                                                             String address,
                                                             boolean tradeInstant) {
        return paymentAccountsService.createCryptoCurrencyPaymentAccount(accountName,
                currencyCode,
                address,
                tradeInstant);
    }

    public List<PaymentMethod> getCryptoCurrencyPaymentMethods() {
        return paymentAccountsService.getCryptoCurrencyPaymentMethods();
    }

    public void validateFormField(PaymentAccountForm form, PaymentAccountFormField.FieldId fieldId, String value) {
        paymentAccountsService.validateFormField(form, fieldId, value);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Prices
    ///////////////////////////////////////////////////////////////////////////////////////////

    public double getMarketPrice(String currencyCode) throws ExecutionException, InterruptedException, TimeoutException {
        return corePriceService.getMarketPrice(currencyCode);
    }

    public List<MarketPriceInfo> getMarketPrices() throws ExecutionException, InterruptedException, TimeoutException {
        return corePriceService.getMarketPrices();
    }

    public MarketDepthInfo getMarketDepth(String currencyCode) throws ExecutionException, InterruptedException, TimeoutException {
        return corePriceService.getMarketDepth(currencyCode);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Trades
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void takeOffer(String offerId,
                          String paymentAccountId,
                          long amountAsLong,
                          Consumer<Trade> resultHandler,
                          ErrorMessageHandler errorMessageHandler) {
        Offer offer = coreOffersService.getOffer(offerId);
        coreTradesService.takeOffer(offer, paymentAccountId, amountAsLong, resultHandler, errorMessageHandler);
    }

    public void confirmPaymentSent(String tradeId,
                                      ResultHandler resultHandler,
                                      ErrorMessageHandler errorMessageHandler) {
        coreTradesService.confirmPaymentSent(tradeId, resultHandler, errorMessageHandler);
    }

    public void confirmPaymentReceived(String tradeId,
                                       ResultHandler resultHandler,
                                       ErrorMessageHandler errorMessageHandler) {
        coreTradesService.confirmPaymentReceived(tradeId, resultHandler, errorMessageHandler);
    }

    public void closeTrade(String tradeId) {
        coreTradesService.closeTrade(tradeId);
    }

    public Trade getTrade(String tradeId) {
        return coreTradesService.getTrade(tradeId);
    }

    public List<Trade> getTrades() {
        return coreTradesService.getTrades();
    }

    public String getTradeRole(String tradeId) {
        return coreTradesService.getTradeRole(tradeId);
    }

    public List<ChatMessage> getChatMessages(String tradeId) {
        return coreTradesService.getChatMessages(tradeId);
    }

    public void sendChatMessage(String tradeId, String message) {
        coreTradesService.sendChatMessage(tradeId, message);
    }
}
