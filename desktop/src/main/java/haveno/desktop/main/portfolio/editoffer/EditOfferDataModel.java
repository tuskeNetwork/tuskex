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

package tuskex.desktop.main.portfolio.editoffer;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.common.handlers.ResultHandler;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.offer.CreateOfferService;
import tuskex.core.offer.Offer;
import tuskex.core.offer.OfferDirection;
import tuskex.core.offer.OfferPayload;
import tuskex.core.offer.OfferUtil;
import tuskex.core.offer.OpenOffer;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.proto.persistable.CorePersistenceProtoResolver;
import tuskex.core.provider.price.PriceFeedService;
import tuskex.core.trade.statistics.TradeStatisticsManager;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.core.util.coin.CoinUtil;
import tuskex.core.tsk.wallet.Restrictions;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.Navigation;
import tuskex.desktop.main.offer.MutableOfferDataModel;
import tuskex.network.p2p.P2PService;
import java.util.Optional;
import java.util.Set;

class EditOfferDataModel extends MutableOfferDataModel {

    private final CorePersistenceProtoResolver corePersistenceProtoResolver;
    private OpenOffer openOffer;
    private OpenOffer.State initialState;

    @Inject
    EditOfferDataModel(CreateOfferService createOfferService,
                       OpenOfferManager openOfferManager,
                       OfferUtil offerUtil,
                       TskWalletService tskWalletService,
                       Preferences preferences,
                       User user,
                       P2PService p2PService,
                       PriceFeedService priceFeedService,
                       AccountAgeWitnessService accountAgeWitnessService,
                       @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter btcFormatter,
                       CorePersistenceProtoResolver corePersistenceProtoResolver,
                       TradeStatisticsManager tradeStatisticsManager,
                       Navigation navigation) {

        super(createOfferService,
                openOfferManager,
                offerUtil,
                tskWalletService,
                preferences,
                user,
                p2PService,
                priceFeedService,
                accountAgeWitnessService,
                btcFormatter,
                tradeStatisticsManager,
                navigation);
        this.corePersistenceProtoResolver = corePersistenceProtoResolver;
    }

    public void reset() {
        direction = null;
        tradeCurrency = null;
        tradeCurrencyCode.set(null);
        useMarketBasedPrice.set(false);
        amount.set(null);
        minAmount.set(null);
        price.set(null);
        volume.set(null);
        minVolume.set(null);
        buyerSecurityDepositPct.set(0);
        paymentAccounts.clear();
        paymentAccount = null;
        marketPriceMargin = 0;
    }

    public void applyOpenOffer(OpenOffer openOffer) {
        this.openOffer = openOffer;

        Offer offer = openOffer.getOffer();
        direction = offer.getDirection();
        CurrencyUtil.getTradeCurrency(offer.getCurrencyCode())
                .ifPresent(c -> this.tradeCurrency = c);
        tradeCurrencyCode.set(offer.getCurrencyCode());

        this.initialState = openOffer.getState();
        PaymentAccount tmpPaymentAccount = user.getPaymentAccount(openOffer.getOffer().getMakerPaymentAccountId());
        Optional<TradeCurrency> optionalTradeCurrency = CurrencyUtil.getTradeCurrency(openOffer.getOffer().getCurrencyCode());
        if (optionalTradeCurrency.isPresent() && tmpPaymentAccount != null) {
            TradeCurrency selectedTradeCurrency = optionalTradeCurrency.get();
            this.paymentAccount = PaymentAccount.fromProto(tmpPaymentAccount.toProtoMessage(), corePersistenceProtoResolver);
            if (paymentAccount.getSingleTradeCurrency() != null)
                paymentAccount.setSingleTradeCurrency(selectedTradeCurrency);
            else
                paymentAccount.setSelectedTradeCurrency(selectedTradeCurrency);
        }
        
        // TODO: update for TSK to use percent as double?

        // If the security deposit got bounded because it was below the coin amount limit, it can be bigger
        // by percentage than the restriction. We can't determine the percentage originally entered at offer
        // creation, so just use the default value as it doesn't matter anyway.
        double buyerSecurityDepositPercent = CoinUtil.getAsPercentPerBtc(offer.getMaxBuyerSecurityDeposit(), offer.getAmount());
        if (buyerSecurityDepositPercent > Restrictions.getMaxBuyerSecurityDepositAsPercent()
                && offer.getMaxBuyerSecurityDeposit().equals(Restrictions.getMinBuyerSecurityDeposit()))
            buyerSecurityDepositPct.set(Restrictions.getDefaultBuyerSecurityDepositAsPercent());
        else
            buyerSecurityDepositPct.set(buyerSecurityDepositPercent);

        allowAmountUpdate = false;
    }

    @Override
    public boolean initWithData(OfferDirection direction, TradeCurrency tradeCurrency) {
        try {
            return super.initWithData(direction, tradeCurrency);
        } catch (NullPointerException e) {
            if (e.getMessage().contains("tradeCurrency")) {
                throw new IllegalArgumentException("Offers of removed assets cannot be edited. You can only cancel it.", e);
            }
            return false;
        }
    }

    @Override
    protected PaymentAccount getPreselectedPaymentAccount() {
        return paymentAccount;
    }

    public void populateData() {
        Offer offer = openOffer.getOffer();
        // Min amount need to be set before amount as if minAmount is null it would be set by amount
        setMinAmount(offer.getMinAmount());
        setAmount(offer.getAmount());
        setPrice(offer.getPrice());
        setVolume(offer.getVolume());
        setUseMarketBasedPrice(offer.isUseMarketBasedPrice());
        setTriggerPrice(openOffer.getTriggerPrice());
        if (offer.isUseMarketBasedPrice()) {
            setMarketPriceMarginPct(offer.getMarketPriceMarginPct());
        }
    }

    public void onStartEditOffer(ErrorMessageHandler errorMessageHandler) {
        openOfferManager.editOpenOfferStart(openOffer, () -> {
        }, errorMessageHandler);
    }

    public void onPublishOffer(ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        // editedPayload is a merge of the original offerPayload and newOfferPayload
        // fields which are editable are merged in from newOfferPayload (such as payment account details)
        // fields which cannot change (most importantly BTC amount) are sourced from the original offerPayload
        final OfferPayload offerPayload = openOffer.getOffer().getOfferPayload();
        final OfferPayload newOfferPayload = createAndGetOffer().getOfferPayload();
        final OfferPayload editedPayload = new OfferPayload(offerPayload.getId(),
                offerPayload.getDate(),
                offerPayload.getOwnerNodeAddress(),
                offerPayload.getPubKeyRing(),
                offerPayload.getDirection(),
                newOfferPayload.getPrice(),
                newOfferPayload.getMarketPriceMarginPct(),
                newOfferPayload.isUseMarketBasedPrice(),
                offerPayload.getAmount(),
                offerPayload.getMinAmount(),
                offerPayload.getMakerFeePct(),
                offerPayload.getTakerFeePct(),
                offerPayload.getPenaltyFeePct(),
                offerPayload.getBuyerSecurityDepositPct(),
                offerPayload.getSellerSecurityDepositPct(),
                newOfferPayload.getBaseCurrencyCode(),
                newOfferPayload.getCounterCurrencyCode(),
                newOfferPayload.getPaymentMethodId(),
                newOfferPayload.getMakerPaymentAccountId(),
                newOfferPayload.getCountryCode(),
                newOfferPayload.getAcceptedCountryCodes(),
                newOfferPayload.getBankId(),
                newOfferPayload.getAcceptedBankIds(),
                offerPayload.getVersionNr(),
                offerPayload.getBlockHeightAtOfferCreation(),
                offerPayload.getMaxTradeLimit(),
                offerPayload.getMaxTradePeriod(),
                offerPayload.isUseAutoClose(),
                offerPayload.isUseReOpenAfterAutoClose(),
                offerPayload.getLowerClosePrice(),
                offerPayload.getUpperClosePrice(),
                offerPayload.isPrivateOffer(),
                offerPayload.getHashOfChallenge(),
                offerPayload.getExtraDataMap(),
                offerPayload.getProtocolVersion(),
                offerPayload.getArbitratorSigner(),
                offerPayload.getArbitratorSignature(),
                offerPayload.getReserveTxKeyImages());

        final Offer editedOffer = new Offer(editedPayload);
        editedOffer.setPriceFeedService(priceFeedService);
        editedOffer.setState(Offer.State.AVAILABLE);

        openOfferManager.editOpenOfferPublish(editedOffer, triggerPrice, initialState, () -> {
            openOffer = null;
            resultHandler.handleResult();
        }, errorMessageHandler);
    }

    public void onCancelEditOffer(ErrorMessageHandler errorMessageHandler) {
        if (openOffer != null)
            openOfferManager.editOpenOfferCancel(openOffer, initialState, () -> {
            }, errorMessageHandler);
    }

    @Override
    protected Set<PaymentAccount> getUserPaymentAccounts() {
        throw new RuntimeException("Edit offer not supported with TSK");
    }
}
