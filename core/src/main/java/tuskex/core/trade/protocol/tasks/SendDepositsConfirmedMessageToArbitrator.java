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

import tuskex.common.crypto.PubKeyRing;
import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.trade.Trade;
import tuskex.network.p2p.NodeAddress;
import lombok.extern.slf4j.Slf4j;

/**
 * Send message on first confirmation to decrypt peer payment account and update multisig hex.
 */
@Slf4j
public class SendDepositsConfirmedMessageToArbitrator extends SendDepositsConfirmedMessage {

    public SendDepositsConfirmedMessageToArbitrator(TaskRunner<Trade> taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    public NodeAddress getReceiverNodeAddress() {
        return trade.getArbitrator().getNodeAddress();
    }

    @Override
    public PubKeyRing getReceiverPubKeyRing() {
        return trade.getArbitrator().getPubKeyRing();
    }
}
