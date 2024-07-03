package tuskex.core.trade.protocol;

import tuskex.core.trade.messages.TradeMessage;
import tuskex.network.p2p.AckMessage;
import tuskex.network.p2p.NodeAddress;

/**
 * Receives notifications of decrypted, verified trade and ack messages.
 */
public class TradeListener {
  public void onVerifiedTradeMessage(TradeMessage message, NodeAddress sender) { }
  public void onAckMessage(AckMessage ackMessage, NodeAddress sender) { }
}
