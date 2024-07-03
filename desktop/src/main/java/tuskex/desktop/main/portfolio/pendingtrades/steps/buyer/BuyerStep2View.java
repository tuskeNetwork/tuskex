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

package tuskex.desktop.main.portfolio.pendingtrades.steps.buyer;

import tuskex.common.Timer;
import tuskex.common.UserThread;
import tuskex.common.app.DevEnv;
import tuskex.common.util.Tuple4;
import tuskex.core.locale.Res;
import tuskex.core.offer.Offer;
import tuskex.core.payment.PaymentAccount;
import tuskex.core.payment.PaymentAccountUtil;
import tuskex.core.payment.payload.AssetAccountPayload;
import tuskex.core.payment.payload.CashDepositAccountPayload;
import tuskex.core.payment.payload.F2FAccountPayload;
import tuskex.core.payment.payload.FasterPaymentsAccountPayload;
import tuskex.core.payment.payload.HalCashAccountPayload;
import tuskex.core.payment.payload.MoneyGramAccountPayload;
import tuskex.core.payment.payload.PayByMailAccountPayload;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.payload.PaymentMethod;
import tuskex.core.payment.payload.SwiftAccountPayload;
import tuskex.core.payment.payload.USPostalMoneyOrderAccountPayload;
import tuskex.core.payment.payload.WesternUnionAccountPayload;
import tuskex.core.trade.Trade;
import tuskex.core.user.DontShowAgainLookup;
import tuskex.core.util.VolumeUtil;
import tuskex.desktop.components.BusyAnimation;
import tuskex.desktop.components.TextFieldWithCopyIcon;
import tuskex.desktop.components.TitledGroupBg;
import tuskex.desktop.components.paymentmethods.AchTransferForm;
import tuskex.desktop.components.paymentmethods.AdvancedCashForm;
import tuskex.desktop.components.paymentmethods.AliPayForm;
import tuskex.desktop.components.paymentmethods.AmazonGiftCardForm;
import tuskex.desktop.components.paymentmethods.AssetsForm;
import tuskex.desktop.components.paymentmethods.AustraliaPayidForm;
import tuskex.desktop.components.paymentmethods.BizumForm;
import tuskex.desktop.components.paymentmethods.CapitualForm;
import tuskex.desktop.components.paymentmethods.CashAppForm;
import tuskex.desktop.components.paymentmethods.CashAtAtmForm;
import tuskex.desktop.components.paymentmethods.PayByMailForm;
import tuskex.desktop.components.paymentmethods.PayPalForm;
import tuskex.desktop.components.paymentmethods.CashDepositForm;
import tuskex.desktop.components.paymentmethods.CelPayForm;
import tuskex.desktop.components.paymentmethods.ChaseQuickPayForm;
import tuskex.desktop.components.paymentmethods.ZelleForm;
import tuskex.desktop.components.paymentmethods.DomesticWireTransferForm;
import tuskex.desktop.components.paymentmethods.F2FForm;
import tuskex.desktop.components.paymentmethods.FasterPaymentsForm;
import tuskex.desktop.components.paymentmethods.HalCashForm;
import tuskex.desktop.components.paymentmethods.ImpsForm;
import tuskex.desktop.components.paymentmethods.InteracETransferForm;
import tuskex.desktop.components.paymentmethods.JapanBankTransferForm;
import tuskex.desktop.components.paymentmethods.MoneseForm;
import tuskex.desktop.components.paymentmethods.MoneyBeamForm;
import tuskex.desktop.components.paymentmethods.MoneyGramForm;
import tuskex.desktop.components.paymentmethods.NationalBankForm;
import tuskex.desktop.components.paymentmethods.NeftForm;
import tuskex.desktop.components.paymentmethods.NequiForm;
import tuskex.desktop.components.paymentmethods.PaxumForm;
import tuskex.desktop.components.paymentmethods.PayseraForm;
import tuskex.desktop.components.paymentmethods.PaytmForm;
import tuskex.desktop.components.paymentmethods.PerfectMoneyForm;
import tuskex.desktop.components.paymentmethods.PixForm;
import tuskex.desktop.components.paymentmethods.PopmoneyForm;
import tuskex.desktop.components.paymentmethods.PromptPayForm;
import tuskex.desktop.components.paymentmethods.RevolutForm;
import tuskex.desktop.components.paymentmethods.RtgsForm;
import tuskex.desktop.components.paymentmethods.SameBankForm;
import tuskex.desktop.components.paymentmethods.SatispayForm;
import tuskex.desktop.components.paymentmethods.SepaForm;
import tuskex.desktop.components.paymentmethods.SepaInstantForm;
import tuskex.desktop.components.paymentmethods.SpecificBankForm;
import tuskex.desktop.components.paymentmethods.StrikeForm;
import tuskex.desktop.components.paymentmethods.SwiftForm;
import tuskex.desktop.components.paymentmethods.SwishForm;
import tuskex.desktop.components.paymentmethods.TikkieForm;
import tuskex.desktop.components.paymentmethods.TransferwiseForm;
import tuskex.desktop.components.paymentmethods.TransferwiseUsdForm;
import tuskex.desktop.components.paymentmethods.USPostalMoneyOrderForm;
import tuskex.desktop.components.paymentmethods.UpholdForm;
import tuskex.desktop.components.paymentmethods.UpiForm;
import tuskex.desktop.components.paymentmethods.VenmoForm;
import tuskex.desktop.components.paymentmethods.VerseForm;
import tuskex.desktop.components.paymentmethods.WeChatPayForm;
import tuskex.desktop.components.paymentmethods.WesternUnionForm;
import tuskex.desktop.main.overlays.popups.Popup;
import tuskex.desktop.main.portfolio.pendingtrades.PendingTradesViewModel;
import tuskex.desktop.main.portfolio.pendingtrades.steps.TradeStepView;
import tuskex.desktop.util.Layout;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static tuskex.desktop.util.FormBuilder.addButtonBusyAnimationLabel;
import static tuskex.desktop.util.FormBuilder.addCompactTopLabelTextFieldWithCopyIcon;
import static tuskex.desktop.util.FormBuilder.addTitledGroupBg;
import static tuskex.desktop.util.FormBuilder.addTopLabelTextFieldWithCopyIcon;

public class BuyerStep2View extends TradeStepView {

    private Button confirmButton;
    private Label statusLabel;
    private BusyAnimation busyAnimation;
    private Subscription tradeStatePropertySubscription;
    private Timer timeoutTimer;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, Initialisation
    ///////////////////////////////////////////////////////////////////////////////////////////

    public BuyerStep2View(PendingTradesViewModel model) {
        super(model);
    }

    @Override
    public void activate() {
        super.activate();

        if (timeoutTimer != null)
            timeoutTimer.stop();

        //TODO we get called twice, check why
        if (tradeStatePropertySubscription == null) {
            tradeStatePropertySubscription = EasyBind.subscribe(trade.stateProperty(), state -> {
                if (timeoutTimer != null)
                    timeoutTimer.stop();

                if (trade.isDepositsUnlocked() && !trade.isPaymentSent()) {
                    busyAnimation.stop();
                    statusLabel.setText("");
                    showPopup();
                } else if (state.ordinal() <= Trade.State.SELLER_RECEIVED_PAYMENT_SENT_MSG.ordinal()) {
                    switch (state) {
                        case BUYER_CONFIRMED_PAYMENT_SENT:
                            busyAnimation.play();
                            statusLabel.setText(Res.get("shared.preparingConfirmation"));
                            break;
                        case BUYER_SENT_PAYMENT_SENT_MSG:
                            busyAnimation.play();
                            statusLabel.setText(Res.get("shared.sendingConfirmation"));
                            timeoutTimer = UserThread.runAfter(() -> {
                                busyAnimation.stop();
                                statusLabel.setText(Res.get("shared.sendingConfirmationAgain"));
                            }, 30);
                            break;
                        case BUYER_STORED_IN_MAILBOX_PAYMENT_SENT_MSG:
                            busyAnimation.stop();
                            statusLabel.setText(Res.get("shared.messageStoredInMailbox"));
                            break;
                        case BUYER_SAW_ARRIVED_PAYMENT_SENT_MSG:
                        case SELLER_RECEIVED_PAYMENT_SENT_MSG:
                            busyAnimation.stop();
                            statusLabel.setText(Res.get("shared.messageArrived"));
                            break;
                        case BUYER_SEND_FAILED_PAYMENT_SENT_MSG:
                            // We get a popup and the trade closed, so we dont need to show anything here
                            busyAnimation.stop();
                            statusLabel.setText("");
                            break;
                        default:
                            log.warn("Unexpected case: State={}, tradeId={} ", state.name(), trade.getId());
                            busyAnimation.stop();
                            statusLabel.setText(Res.get("shared.sendingConfirmationAgain"));
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();

        busyAnimation.stop();

        if (timeoutTimer != null)
            timeoutTimer.stop();

        if (tradeStatePropertySubscription != null) {
            tradeStatePropertySubscription.unsubscribe();
            tradeStatePropertySubscription = null;
        }
    }

    @Override
    protected void onPendingTradesInitialized() {
        super.onPendingTradesInitialized();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Content
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void addContent() {
        gridPane.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);

        addTradeInfoBlock();


        PaymentAccountPayload paymentAccountPayload = model.dataModel.getSellersPaymentAccountPayload();
        String paymentMethodId = paymentAccountPayload != null ? paymentAccountPayload.getPaymentMethodId() : "<pending>";
        TitledGroupBg accountTitledGroupBg = addTitledGroupBg(gridPane, ++gridRow, 4,
                Res.get("portfolio.pending.step2_buyer.startPaymentUsing", Res.get(paymentMethodId)),
                Layout.COMPACT_GROUP_DISTANCE);
        TextFieldWithCopyIcon field = addTopLabelTextFieldWithCopyIcon(gridPane, gridRow, 0,
                Res.get("portfolio.pending.step2_buyer.amountToTransfer"),
                model.getFiatVolume(),
                Layout.COMPACT_FIRST_ROW_AND_GROUP_DISTANCE).second;
        field.setCopyWithoutCurrencyPostFix(true);

        //preland: this fixes a textarea layout glitch
        TextArea uiHack = new TextArea();
        uiHack.setMaxHeight(1);
        GridPane.setRowIndex(uiHack, 1);
        GridPane.setMargin(uiHack, new Insets(0, 0, 0, 0));
        uiHack.setVisible(false);
        gridPane.getChildren().add(uiHack);

        switch (paymentMethodId) {
            case PaymentMethod.UPHOLD_ID:
                gridRow = UpholdForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.MONEY_BEAM_ID:
                gridRow = MoneyBeamForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.POPMONEY_ID:
                gridRow = PopmoneyForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.REVOLUT_ID:
                gridRow = RevolutForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.PERFECT_MONEY_ID:
                gridRow = PerfectMoneyForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.SEPA_ID:
                gridRow = SepaForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.SEPA_INSTANT_ID:
                gridRow = SepaInstantForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.FASTER_PAYMENTS_ID:
                gridRow = FasterPaymentsForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.NATIONAL_BANK_ID:
                gridRow = NationalBankForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.AUSTRALIA_PAYID_ID:
                gridRow = AustraliaPayidForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.SAME_BANK_ID:
                gridRow = SameBankForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.SPECIFIC_BANKS_ID:
                gridRow = SpecificBankForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.SWISH_ID:
                gridRow = SwishForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.ALI_PAY_ID:
                gridRow = AliPayForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.WECHAT_PAY_ID:
                gridRow = WeChatPayForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.ZELLE_ID:
                gridRow = ZelleForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.CHASE_QUICK_PAY_ID:
                gridRow = ChaseQuickPayForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.INTERAC_E_TRANSFER_ID:
                gridRow = InteracETransferForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.JAPAN_BANK_ID:
                gridRow = JapanBankTransferForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.US_POSTAL_MONEY_ORDER_ID:
                gridRow = USPostalMoneyOrderForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.CASH_DEPOSIT_ID:
                gridRow = CashDepositForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.PAY_BY_MAIL_ID:
                gridRow = PayByMailForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.CASH_AT_ATM_ID:
                gridRow = CashAtAtmForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.MONEY_GRAM_ID:
                gridRow = MoneyGramForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.WESTERN_UNION_ID:
                gridRow = WesternUnionForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.HAL_CASH_ID:
                gridRow = HalCashForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.F2F_ID:
                checkNotNull(model.dataModel.getTrade(), "model.dataModel.getTrade() must not be null");
                checkNotNull(model.dataModel.getTrade().getOffer(), "model.dataModel.getTrade().getOffer() must not be null");
                gridRow = F2FForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload, model.dataModel.getTrade().getOffer(), 0);
                break;
            case PaymentMethod.BLOCK_CHAINS_ID:
            case PaymentMethod.BLOCK_CHAINS_INSTANT_ID:
                String labelTitle = Res.get("portfolio.pending.step2_buyer.sellersAddress", getCurrencyName(trade));
                gridRow = AssetsForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload, labelTitle);
                break;
            case PaymentMethod.PROMPT_PAY_ID:
                gridRow = PromptPayForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.ADVANCED_CASH_ID:
                gridRow = AdvancedCashForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.TRANSFERWISE_ID:
                gridRow = TransferwiseForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.TRANSFERWISE_USD_ID:
                gridRow = TransferwiseUsdForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.PAYSERA_ID:
                gridRow = PayseraForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.PAXUM_ID:
                gridRow = PaxumForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.NEFT_ID:
                gridRow = NeftForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.RTGS_ID:
                gridRow = RtgsForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.IMPS_ID:
                gridRow = ImpsForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.UPI_ID:
                gridRow = UpiForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.PAYTM_ID:
                gridRow = PaytmForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.NEQUI_ID:
                gridRow = NequiForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.BIZUM_ID:
                gridRow = BizumForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.PIX_ID:
                gridRow = PixForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.AMAZON_GIFT_CARD_ID:
                gridRow = AmazonGiftCardForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.CAPITUAL_ID:
                gridRow = CapitualForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.CELPAY_ID:
                gridRow = CelPayForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.MONESE_ID:
                gridRow = MoneseForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.SATISPAY_ID:
                gridRow = SatispayForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.TIKKIE_ID:
                gridRow = TikkieForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.VERSE_ID:
                gridRow = VerseForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.STRIKE_ID:
                gridRow = StrikeForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.SWIFT_ID:
                gridRow = SwiftForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload, trade);
                break;
            case PaymentMethod.ACH_TRANSFER_ID:
                gridRow = AchTransferForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.DOMESTIC_WIRE_TRANSFER_ID:
                gridRow = DomesticWireTransferForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.CASH_APP_ID:
                gridRow = CashAppForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.PAYPAL_ID:
                gridRow = PayPalForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            case PaymentMethod.VENMO_ID:
                gridRow = VenmoForm.addFormForBuyer(gridPane, gridRow, paymentAccountPayload);
                break;
            default:
                log.error("Not supported PaymentMethod: " + paymentMethodId);
        }

        Trade trade = model.getTrade();
        if (trade != null && model.getUser().getPaymentAccounts() != null) {
            Offer offer = trade.getOffer();
            List<PaymentAccount> possiblePaymentAccounts = PaymentAccountUtil.getPossiblePaymentAccounts(offer,
                    model.getUser().getPaymentAccounts(), model.dataModel.getAccountAgeWitnessService());
            PaymentAccountPayload buyersPaymentAccountPayload = model.dataModel.getBuyersPaymentAccountPayload();
            if (buyersPaymentAccountPayload != null && possiblePaymentAccounts.size() > 1) {
                String id = buyersPaymentAccountPayload.getId();
                possiblePaymentAccounts.stream()
                        .filter(paymentAccount -> paymentAccount.getId().equals(id))
                        .findFirst()
                        .ifPresent(paymentAccount -> {
                            String accountName = paymentAccount.getAccountName();
                            addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, 0,
                                    Res.get("portfolio.pending.step2_buyer.buyerAccount"), accountName);
                        });
            }
        }

        GridPane.setRowSpan(accountTitledGroupBg, gridRow - 1);

        Tuple4<Button, BusyAnimation, Label, HBox> tuple3 = addButtonBusyAnimationLabel(gridPane, ++gridRow, 0,
                Res.get("portfolio.pending.step2_buyer.paymentSent"), 10);

        HBox hBox = tuple3.fourth;
        GridPane.setColumnSpan(hBox, 2);
        confirmButton = tuple3.first;
        confirmButton.setDisable(!confirmPaymentSentPermitted());
        confirmButton.setOnAction(e -> onPaymentSent());
        busyAnimation = tuple3.second;
        statusLabel = tuple3.third;
    }

    private boolean confirmPaymentSentPermitted() {
        if (!trade.confirmPermitted()) return false;
        if (trade.getState() == Trade.State.BUYER_SEND_FAILED_PAYMENT_SENT_MSG) return true;
        return trade.isDepositsUnlocked() && trade.getState().ordinal() < Trade.State.BUYER_CONFIRMED_PAYMENT_SENT.ordinal();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Warning
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected String getFirstHalfOverWarnText() {
        return Res.get("portfolio.pending.step2_buyer.warn",
                getCurrencyCode(trade),
                model.getDateForOpenDispute());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Dispute
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected String getPeriodOverWarnText() {
        return Res.get("portfolio.pending.step2_buyer.openForDispute");
    }

    @Override
    protected void applyOnDisputeOpened() {
    }

    @Override
    protected void updateDisputeState(Trade.DisputeState disputeState) {
        super.updateDisputeState(disputeState);
        confirmButton.setDisable(!confirmPaymentSentPermitted());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI Handlers
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void onPaymentSent() {
        if (!model.dataModel.isBootstrappedOrShowPopup()) {
            return;
        }

        if (!model.dataModel.isReadyForTxBroadcast()) {
            return;
        }

        PaymentAccountPayload sellersPaymentAccountPayload = model.dataModel.getSellersPaymentAccountPayload();
        Trade trade = checkNotNull(model.dataModel.getTrade(), "trade must not be null");
        if (sellersPaymentAccountPayload instanceof CashDepositAccountPayload) {
            String key = "confirmPaperReceiptSent";
            if (!DevEnv.isDevMode() && DontShowAgainLookup.showAgain(key)) {
                Popup popup = new Popup();
                popup.headLine(Res.get("portfolio.pending.step2_buyer.paperReceipt.headline"))
                        .feedback(Res.get("portfolio.pending.step2_buyer.paperReceipt.msg"))
                        .onAction(this::showConfirmPaymentSentPopup)
                        .closeButtonText(Res.get("shared.no"))
                        .onClose(popup::hide)
                        .dontShowAgainId(key)
                        .show();
            } else {
                showConfirmPaymentSentPopup();
            }
        } else if (sellersPaymentAccountPayload instanceof WesternUnionAccountPayload) {
            String key = "westernUnionMTCNSent";
            if (!DevEnv.isDevMode() && DontShowAgainLookup.showAgain(key)) {
                String email = ((WesternUnionAccountPayload) sellersPaymentAccountPayload).getEmail();
                Popup popup = new Popup();
                popup.headLine(Res.get("portfolio.pending.step2_buyer.westernUnionMTCNInfo.headline"))
                        .feedback(Res.get("portfolio.pending.step2_buyer.westernUnionMTCNInfo.msg", email))
                        .onAction(this::showConfirmPaymentSentPopup)
                        .actionButtonText(Res.get("shared.yes"))
                        .closeButtonText(Res.get("shared.no"))
                        .onClose(popup::hide)
                        .dontShowAgainId(key)
                        .show();
            } else {
                showConfirmPaymentSentPopup();
            }
        } else if (sellersPaymentAccountPayload instanceof MoneyGramAccountPayload) {
            String key = "moneyGramMTCNSent";
            if (!DevEnv.isDevMode() && DontShowAgainLookup.showAgain(key)) {
                String email = ((MoneyGramAccountPayload) sellersPaymentAccountPayload).getEmail();
                Popup popup = new Popup();
                popup.headLine(Res.get("portfolio.pending.step2_buyer.moneyGramMTCNInfo.headline"))
                        .feedback(Res.get("portfolio.pending.step2_buyer.moneyGramMTCNInfo.msg", email))
                        .onAction(this::showConfirmPaymentSentPopup)
                        .actionButtonText(Res.get("shared.yes"))
                        .closeButtonText(Res.get("shared.no"))
                        .onClose(popup::hide)
                        .dontShowAgainId(key)
                        .show();
            } else {
                showConfirmPaymentSentPopup();
            }
        } else if (sellersPaymentAccountPayload instanceof HalCashAccountPayload) {
            String key = "halCashCodeInfo";
            if (!DevEnv.isDevMode() && DontShowAgainLookup.showAgain(key)) {
                String mobileNr = ((HalCashAccountPayload) sellersPaymentAccountPayload).getMobileNr();
                Popup popup = new Popup();
                popup.headLine(Res.get("portfolio.pending.step2_buyer.halCashInfo.headline"))
                        .feedback(Res.get("portfolio.pending.step2_buyer.halCashInfo.msg",
                                trade.getShortId(), mobileNr))
                        .onAction(this::showConfirmPaymentSentPopup)
                        .actionButtonText(Res.get("shared.yes"))
                        .closeButtonText(Res.get("shared.no"))
                        .onClose(popup::hide)
                        .dontShowAgainId(key)
                        .show();
            } else {
                showConfirmPaymentSentPopup();
            }
        } else {
            showConfirmPaymentSentPopup();
        }
    }

    private void showConfirmPaymentSentPopup() {
        String key = "confirmPaymentSent";
        if (!DevEnv.isDevMode() && DontShowAgainLookup.showAgain(key)) {
            Popup popup = new Popup();
            popup.headLine(Res.get("portfolio.pending.step2_buyer.confirmStart.headline"))
                    .confirmation(Res.get("portfolio.pending.step2_buyer.confirmStart.msg", getCurrencyName(trade)))
                    .width(700)
                    .actionButtonText(Res.get("portfolio.pending.step2_buyer.confirmStart.yes"))
                    .onAction(this::confirmPaymentSent)
                    .closeButtonText(Res.get("shared.no"))
                    .onClose(popup::hide)
                    .dontShowAgainId(key)
                    .show();
        } else {
            confirmPaymentSent();
        }
    }

    private void confirmPaymentSent() {
        busyAnimation.play();
        statusLabel.setText(Res.get("shared.preparingConfirmation"));
        confirmButton.setDisable(true);

        model.dataModel.onPaymentSent(() -> {
        }, errorMessage -> {
            busyAnimation.stop();
            new Popup().warning(Res.get("popup.warning.sendMsgFailed") + "\n\n" + errorMessage).show();
            confirmButton.setDisable(!confirmPaymentSentPermitted());
            UserThread.execute(() -> statusLabel.setText("Error confirming payment sent."));
        });
    }

    private void showPopup() {
        PaymentAccountPayload paymentAccountPayload = model.dataModel.getSellersPaymentAccountPayload();
        if (paymentAccountPayload != null) {
            String message = Res.get("portfolio.pending.step2.confReached");
            String refTextWarn = Res.get("portfolio.pending.step2_buyer.refTextWarn");
            String fees = Res.get("portfolio.pending.step2_buyer.fees");
            String id = trade.getShortId();
            String amount = VolumeUtil.formatVolumeWithCode(trade.getVolume());
            if (paymentAccountPayload instanceof AssetAccountPayload) {
                message += Res.get("portfolio.pending.step2_buyer.crypto",
                        getCurrencyName(trade),
                        amount);
            } else if (paymentAccountPayload instanceof CashDepositAccountPayload) {
                message += Res.get("portfolio.pending.step2_buyer.cash",
                        amount) +
                        refTextWarn + "\n\n" +
                        fees + "\n\n" +
                        Res.get("portfolio.pending.step2_buyer.cash.extra");
            } else if (paymentAccountPayload instanceof WesternUnionAccountPayload) {
                final String email = ((WesternUnionAccountPayload) paymentAccountPayload).getEmail();
                final String extra = Res.get("portfolio.pending.step2_buyer.westernUnion.extra", email);
                message += Res.get("portfolio.pending.step2_buyer.westernUnion",
                        amount) +
                        extra;
            } else if (paymentAccountPayload instanceof MoneyGramAccountPayload) {
                final String email = ((MoneyGramAccountPayload) paymentAccountPayload).getEmail();
                final String extra = Res.get("portfolio.pending.step2_buyer.moneyGram.extra", email);
                message += Res.get("portfolio.pending.step2_buyer.moneyGram",
                        amount) +
                        extra;
            } else if (paymentAccountPayload instanceof USPostalMoneyOrderAccountPayload) {
                message += Res.get("portfolio.pending.step2_buyer.postal", amount) +
                        refTextWarn;
            } else if (paymentAccountPayload instanceof F2FAccountPayload) {
                message += Res.get("portfolio.pending.step2_buyer.f2f", amount);
            } else if (paymentAccountPayload instanceof FasterPaymentsAccountPayload) {
                message += Res.get("portfolio.pending.step2_buyer.pay", amount) +
                        Res.get("portfolio.pending.step2_buyer.fasterPaymentsHolderNameInfo") + "\n\n" +
                        refTextWarn + "\n\n" +
                        fees;
            } else if (paymentAccountPayload instanceof PayByMailAccountPayload ||
                    paymentAccountPayload instanceof HalCashAccountPayload) {
                message += Res.get("portfolio.pending.step2_buyer.pay", amount);
            } else if (paymentAccountPayload instanceof SwiftAccountPayload) {
                message += Res.get("portfolio.pending.step2_buyer.pay", amount) +
                        refTextWarn + "\n\n" +
                        Res.get("portfolio.pending.step2_buyer.fees.swift");
            } else {
                message += Res.get("portfolio.pending.step2_buyer.pay", amount) +
                        refTextWarn + "\n\n" +
                        fees;
            }

            String key = "startPayment" + trade.getId();
            if (!DevEnv.isDevMode() && DontShowAgainLookup.showAgain(key)) {
                DontShowAgainLookup.dontShowAgain(key, true);
                new Popup().headLine(Res.get("popup.attention.forTradeWithId", id))
                        .attention(message)
                        .show();
            }
        }
    }
}
