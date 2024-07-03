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


import tuskex.common.app.Version;
import tuskex.common.crypto.PubKeyRing;
import tuskex.common.taskrunner.TaskRunner;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.trade.Trade;
import tuskex.core.trade.messages.DepositRequest;
import tuskex.core.trade.messages.SignContractResponse;
import tuskex.core.trade.protocol.TradePeer;
import tuskex.network.p2p.SendDirectMessageListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
public class SendDepositRequest extends TradeTask {

    @SuppressWarnings({"unused"})
    public SendDepositRequest(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // compare contracts
            String contractAsJson = trade.getContractAsJson();
            SignContractResponse response = (SignContractResponse) processModel.getTradeMessage();
            if (!contractAsJson.equals(response.getContractAsJson())) {
                trade.getContract().printDiff(response.getContractAsJson());
                failed("Contracts are not matching");
                return;
            }

            // get peer info
            TradePeer peer = trade.getTradePeer(processModel.getTempTradePeerNodeAddress());
            PubKeyRing peerPubKeyRing = peer.getPubKeyRing();

            // save peer's encrypted payment account payload
            peer.setEncryptedPaymentAccountPayload(response.getEncryptedPaymentAccountPayload());
            if (peer.getEncryptedPaymentAccountPayload() == null) throw new RuntimeException("Peer did not send encrypted payment account payload");

            // verify signature
            // TODO (woodser): transfer contract for convenient comparison?
            byte[] signature = response.getContractSignature();
            if (!TuskexUtils.isSignatureValid(peerPubKeyRing, contractAsJson, signature)) throw new RuntimeException("Peer's contract signature is invalid");

            // set peer's signature
            peer.setContractSignature(signature);

            // send deposit request when all contract signatures received
            if (processModel.getArbitrator().getContractSignature() != null && processModel.getMaker().getContractSignature() != null && processModel.getTaker().getContractSignature() != null) {

                // create request for arbitrator to deposit funds to multisig
                DepositRequest request = new DepositRequest(
                        trade.getOffer().getId(),
                        UUID.randomUUID().toString(),
                        Version.getP2PMessageVersion(),
                        new Date().getTime(),
                        trade.getSelf().getContractSignature(),
                        trade.getSelf().getDepositTx().getFullHex(),
                        trade.getSelf().getDepositTx().getKey(),
                        trade.getSelf().getPaymentAccountKey());

                // update trade state
                trade.setState(Trade.State.SENT_PUBLISH_DEPOSIT_TX_REQUEST);
                processModel.getTradeManager().requestPersistence();

                // send request to arbitrator
                log.info("Sending {} to arbitrator {}; offerId={}; uid={}", request.getClass().getSimpleName(), trade.getArbitrator().getNodeAddress(), trade.getId(), request.getUid());
                processModel.getP2PService().sendEncryptedDirectMessage(trade.getArbitrator().getNodeAddress(), trade.getArbitrator().getPubKeyRing(), request, new SendDirectMessageListener() {
                    @Override
                    public void onArrived() {
                        log.info("{} arrived: arbitrator={}; offerId={}; uid={}", request.getClass().getSimpleName(), trade.getArbitrator().getNodeAddress(), trade.getId(), request.getUid());
                        trade.setStateIfValidTransitionTo(Trade.State.SAW_ARRIVED_PUBLISH_DEPOSIT_TX_REQUEST);
                        processModel.getTradeManager().requestPersistence();
                        trade.addInitProgressStep();
                        complete();
                    }
                    @Override
                    public void onFault(String errorMessage) {
                        log.error("Sending {} failed: uid={}; peer={}; error={}", request.getClass().getSimpleName(), trade.getArbitrator().getNodeAddress(), trade.getId(), errorMessage);
                        appendToErrorMessage("Sending message failed: message=" + request + "\nerrorMessage=" + errorMessage);
                        failed();
                    }
                });
            } else {
                List<String> awaitingSignaturesFrom = new ArrayList<>();
                if (processModel.getArbitrator().getContractSignature() == null) awaitingSignaturesFrom.add("arbitrator");
                if (processModel.getMaker().getContractSignature() == null) awaitingSignaturesFrom.add("maker");
                if (processModel.getTaker().getContractSignature() == null) awaitingSignaturesFrom.add("taker");
                log.info("Waiting for contract signature from {} to send deposit request", awaitingSignaturesFrom);
                complete(); // does not yet have needed signatures
            }
        } catch (Throwable t) {
            failed(t);
        }
    }
}
