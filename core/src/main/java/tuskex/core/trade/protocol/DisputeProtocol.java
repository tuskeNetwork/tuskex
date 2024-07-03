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

package tuskex.core.trade.protocol;

import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.common.handlers.ResultHandler;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.MediatedPayoutTxPublishedMessage;
import tuskex.core.trade.messages.MediatedPayoutTxSignatureMessage;
import tuskex.core.trade.messages.TradeMessage;
import tuskex.core.trade.protocol.tasks.ApplyFilter;
import tuskex.core.trade.protocol.tasks.mediation.FinalizeMediatedPayoutTx;
import tuskex.core.trade.protocol.tasks.mediation.ProcessMediatedPayoutSignatureMessage;
import tuskex.core.trade.protocol.tasks.mediation.ProcessMediatedPayoutTxPublishedMessage;
import tuskex.core.trade.protocol.tasks.mediation.SendMediatedPayoutSignatureMessage;
import tuskex.core.trade.protocol.tasks.mediation.SendMediatedPayoutTxPublishedMessage;
import tuskex.core.trade.protocol.tasks.mediation.SetupMediatedPayoutTxListener;
import tuskex.core.trade.protocol.tasks.mediation.SignMediatedPayoutTx;
import tuskex.network.p2p.NodeAddress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DisputeProtocol extends TradeProtocol {

    enum DisputeEvent implements FluentProtocol.Event {
        MEDIATION_RESULT_ACCEPTED,
        MEDIATION_RESULT_REJECTED,
        ARBITRATION_REQUESTED
    }

    public DisputeProtocol(Trade trade) {
        super(trade);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Dispatcher
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onTradeMessage(TradeMessage message, NodeAddress peer) {
        super.onTradeMessage(message, peer);
        if (message instanceof MediatedPayoutTxSignatureMessage) {
            handle((MediatedPayoutTxSignatureMessage) message, peer);
        } else if (message instanceof MediatedPayoutTxPublishedMessage) {
            handle((MediatedPayoutTxPublishedMessage) message, peer);
        }
    }

    @Override
    protected void onMailboxMessage(TradeMessage message, NodeAddress peer) {
        super.onMailboxMessage(message, peer);
        if (message instanceof MediatedPayoutTxSignatureMessage) {
            handle((MediatedPayoutTxSignatureMessage) message, peer);
        } else if (message instanceof MediatedPayoutTxPublishedMessage) {
            handle((MediatedPayoutTxPublishedMessage) message, peer);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // User interaction: Trader accepts mediation result
    ///////////////////////////////////////////////////////////////////////////////////////////

    // Trader has not yet received the peer's signature but has clicked the accept button.
    public void onAcceptMediationResult(ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        DisputeEvent event = DisputeEvent.MEDIATION_RESULT_ACCEPTED;
        expect(anyPhase(Trade.Phase.DEPOSITS_UNLOCKED,
                Trade.Phase.PAYMENT_SENT,
                Trade.Phase.PAYMENT_RECEIVED)
                .with(event)
                .preCondition(trade.getTradePeer().getMediatedPayoutTxSignature() == null,
                        () -> errorMessageHandler.handleErrorMessage("We have received already the signature from the peer."))
                .preCondition(trade.getPayoutTx() == null,
                        () -> errorMessageHandler.handleErrorMessage("Payout tx is already published.")))
                .setup(tasks(ApplyFilter.class,
                        SignMediatedPayoutTx.class,
                        SendMediatedPayoutSignatureMessage.class,
                        SetupMediatedPayoutTxListener.class)
                        .using(new TradeTaskRunner(trade,
                                () -> {
                                    resultHandler.handleResult();
                                    handleTaskRunnerSuccess(event);
                                },
                                errorMessage -> {
                                    errorMessageHandler.handleErrorMessage(errorMessage);
                                    handleTaskRunnerFault(event, errorMessage);
                                })))
                .executeTasks();
    }

    // Trader has already received the peer's signature and has clicked the accept button as well.
    public void onFinalizeMediationResultPayout(ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        DisputeEvent event = DisputeEvent.MEDIATION_RESULT_ACCEPTED;
        expect(anyPhase(Trade.Phase.DEPOSITS_UNLOCKED,
                Trade.Phase.PAYMENT_SENT,
                Trade.Phase.PAYMENT_RECEIVED)
                .with(event)
                .preCondition(trade.getPayoutTx() == null,
                        () -> errorMessageHandler.handleErrorMessage("Payout tx is already published.")))
                .setup(tasks(ApplyFilter.class,
                        SignMediatedPayoutTx.class,
                        FinalizeMediatedPayoutTx.class,
                        SendMediatedPayoutTxPublishedMessage.class)
                        .using(new TradeTaskRunner(trade,
                                () -> {
                                    resultHandler.handleResult();
                                    handleTaskRunnerSuccess(event);
                                },
                                errorMessage -> {
                                    errorMessageHandler.handleErrorMessage(errorMessage);
                                    handleTaskRunnerFault(event, errorMessage);
                                })))
                .executeTasks();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Mediation: incoming message
    ///////////////////////////////////////////////////////////////////////////////////////////

    protected void handle(MediatedPayoutTxSignatureMessage message, NodeAddress peer) {
        expect(anyPhase(Trade.Phase.DEPOSITS_UNLOCKED,
                Trade.Phase.PAYMENT_SENT,
                Trade.Phase.PAYMENT_RECEIVED)
                .with(message)
                .from(peer))
                .setup(tasks(ProcessMediatedPayoutSignatureMessage.class))
                .executeTasks();
    }

    protected void handle(MediatedPayoutTxPublishedMessage message, NodeAddress peer) {
        expect(anyPhase(Trade.Phase.DEPOSITS_UNLOCKED,
                Trade.Phase.PAYMENT_SENT,
                Trade.Phase.PAYMENT_RECEIVED)
                .with(message)
                .from(peer))
                .setup(tasks(ProcessMediatedPayoutTxPublishedMessage.class))
                .executeTasks();
    }
}