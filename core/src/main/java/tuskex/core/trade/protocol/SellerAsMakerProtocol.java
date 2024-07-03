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

package tuskex.core.trade.protocol;


import tuskex.common.ThreadUtils;
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.core.trade.SellerAsMakerTrade;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.InitTradeRequest;
import tuskex.core.trade.protocol.tasks.ApplyFilter;
import tuskex.core.trade.protocol.tasks.MakerSendInitTradeRequestToArbitrator;
import tuskex.core.trade.protocol.tasks.ProcessInitTradeRequest;
import tuskex.network.p2p.NodeAddress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerAsMakerProtocol extends SellerProtocol implements MakerProtocol {

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public SellerAsMakerProtocol(SellerAsMakerTrade trade) {
        super(trade);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // MakerProtocol
    ///////////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    public void handleInitTradeRequest(InitTradeRequest message,
                                       NodeAddress peer,
                                       ErrorMessageHandler errorMessageHandler) {
        System.out.println(getClass().getCanonicalName() + ".handleInitTradeRequest()");
        ThreadUtils.execute(() -> {
            synchronized (trade) {
                latchTrade();
                this.errorMessageHandler = errorMessageHandler;
                expect(phase(Trade.Phase.INIT)
                        .with(message)
                        .from(peer))
                        .setup(tasks(
                                ApplyFilter.class,
                                ProcessInitTradeRequest.class,
                                MakerSendInitTradeRequestToArbitrator.class)
                        .using(new TradeTaskRunner(trade,
                                () -> {
                                    startTimeout();
                                    handleTaskRunnerSuccess(peer, message);
                                },
                                errorMessage -> {
                                    handleTaskRunnerFault(peer, message, errorMessage);
                                }))
                        .withTimeout(TRADE_STEP_TIMEOUT_SECONDS))
                        .executeTasks(true);
                awaitTradeLatch();
            }
        }, trade.getId());
    }
}
