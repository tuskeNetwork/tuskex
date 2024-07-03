package tuskex.core.trade.protocol;

import tuskex.common.ThreadUtils;
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.core.trade.ArbitratorTrade;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.DepositRequest;
import tuskex.core.trade.messages.DepositResponse;
import tuskex.core.trade.messages.InitTradeRequest;
import tuskex.core.trade.messages.SignContractResponse;
import tuskex.core.trade.messages.TradeMessage;
import tuskex.core.trade.protocol.tasks.ApplyFilter;
import tuskex.core.trade.protocol.tasks.ArbitratorProcessDepositRequest;
import tuskex.core.trade.protocol.tasks.ArbitratorProcessReserveTx;
import tuskex.core.trade.protocol.tasks.ArbitratorSendInitTradeOrMultisigRequests;
import tuskex.core.trade.protocol.tasks.ProcessInitTradeRequest;
import tuskex.core.trade.protocol.tasks.SendDepositsConfirmedMessageToBuyer;
import tuskex.core.trade.protocol.tasks.SendDepositsConfirmedMessageToSeller;
import tuskex.core.trade.protocol.tasks.TradeTask;
import tuskex.core.util.Validator;
import tuskex.network.p2p.NodeAddress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArbitratorProtocol extends DisputeProtocol {

  public ArbitratorProtocol(ArbitratorTrade trade) {
    super(trade);
  }

  @Override
  protected void onTradeMessage(TradeMessage message, NodeAddress peer) {
      super.onTradeMessage(message, peer);
  }

  @Override
  public void onMailboxMessage(TradeMessage message, NodeAddress peer) {
      super.onMailboxMessage(message, peer);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////
  // Incoming messages
  ///////////////////////////////////////////////////////////////////////////////////////////

  public void handleInitTradeRequest(InitTradeRequest message, NodeAddress peer, ErrorMessageHandler errorMessageHandler) {
      System.out.println("ArbitratorProtocol.handleInitTradeRequest()");
      ThreadUtils.execute(() -> {
          synchronized (trade) {
              latchTrade();
              this.errorMessageHandler = errorMessageHandler;
              processModel.setTradeMessage(message); // TODO (woodser): confirm these are null without being set
              expect(phase(Trade.Phase.INIT)
                      .with(message)
                      .from(peer))
                      .setup(tasks(
                              ApplyFilter.class,
                              ProcessInitTradeRequest.class,
                              ArbitratorProcessReserveTx.class,
                              ArbitratorSendInitTradeOrMultisigRequests.class)
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
  
  @Override
  public void handleSignContractResponse(SignContractResponse message, NodeAddress sender) {
      log.warn("Arbitrator ignoring SignContractResponse");
  }
  
  public void handleDepositRequest(DepositRequest request, NodeAddress sender) {
    System.out.println("ArbitratorProtocol.handleDepositRequest() " + trade.getId());
    ThreadUtils.execute(() -> {
        synchronized (trade) {
            latchTrade();
            Validator.checkTradeId(processModel.getOfferId(), request);
            processModel.setTradeMessage(request);
            expect(anyPhase(Trade.Phase.INIT, Trade.Phase.DEPOSIT_REQUESTED)
                .with(request)
                .from(sender))
                .setup(tasks(
                        ArbitratorProcessDepositRequest.class)
                .using(new TradeTaskRunner(trade,
                        () -> {
                            if (trade.getState().ordinal() >= Trade.State.ARBITRATOR_PUBLISHED_DEPOSIT_TXS.ordinal()) {
                                stopTimeout();
                                this.errorMessageHandler = null;
                            }
                            handleTaskRunnerSuccess(sender, request);
                        },
                        errorMessage -> {
                            handleTaskRunnerFault(sender, request, errorMessage);
                        })))
                .executeTasks(true);
            awaitTradeLatch();
        }
    }, trade.getId());
  }
  
  @Override
  public void handleDepositResponse(DepositResponse response, NodeAddress sender) {
      log.warn("Arbitrator ignoring DepositResponse for trade " + response.getOfferId());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<? extends TradeTask>[] getDepositsConfirmedTasks() {
      return new Class[] { SendDepositsConfirmedMessageToBuyer.class, SendDepositsConfirmedMessageToSeller.class };
  }

  @Override
  public void handleError(String errorMessage) {
    // set trade state to send deposit responses with nack
    if (trade instanceof ArbitratorTrade && trade.getState() == Trade.State.SAW_ARRIVED_PUBLISH_DEPOSIT_TX_REQUEST) {
        trade.setStateIfValidTransitionTo(Trade.State.PUBLISH_DEPOSIT_TX_REQUEST_FAILED);
    }
    super.handleError(errorMessage);
  }
}
