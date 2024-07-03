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


import java.math.BigInteger;

import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.DepositResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessDepositResponse extends TradeTask {
    
    @SuppressWarnings({"unused"})
    public ProcessDepositResponse(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
          runInterceptHook();

          // throw if error
          DepositResponse message = (DepositResponse) processModel.getTradeMessage();
          if (message.getErrorMessage() != null) {
            trade.setStateIfValidTransitionTo(Trade.State.PUBLISH_DEPOSIT_TX_REQUEST_FAILED);
            processModel.getTradeManager().unregisterTrade(trade);
            throw new RuntimeException(message.getErrorMessage());
          }

          // record security deposits
          trade.getBuyer().setSecurityDeposit(BigInteger.valueOf(message.getBuyerSecurityDeposit()));
          trade.getSeller().setSecurityDeposit(BigInteger.valueOf(message.getSellerSecurityDeposit()));

          // set success state
          trade.setStateIfValidTransitionTo(Trade.State.ARBITRATOR_PUBLISHED_DEPOSIT_TXS);
          processModel.getTradeManager().requestPersistence();

          // update balances
          trade.getTskWalletService().updateBalanceListeners();
          complete();
        } catch (Throwable t) {
          failed(t);
        }
    }
}
