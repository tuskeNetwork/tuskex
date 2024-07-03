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

package tuskex.core.trade.protocol.tasks;

import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.network.MessageState;
import tuskex.core.trade.Trade;
import tuskex.core.trade.protocol.TradePeer;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class BuyerSendPaymentSentMessageToArbitrator extends BuyerSendPaymentSentMessage {

    public BuyerSendPaymentSentMessageToArbitrator(TaskRunner<Trade> taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected TradePeer getReceiver() {
        return trade.getArbitrator();
    }

    @Override
    protected void setStateSent() {
        complete(); // don't wait for message to arbitrator
    }

    @Override
    protected void setStateFault() {
        // state only updated on seller message
    }

    @Override
    protected void setStateStoredInMailbox() {
        // state only updated on seller message
    }

    @Override
    protected void setStateArrived() {
        // state only updated on seller message
    }

    @Override
    protected boolean isAckedByReceiver() {
        return trade.getProcessModel().getPaymentSentMessageStatePropertyArbitrator().get() == MessageState.ACKNOWLEDGED;
    }
}
