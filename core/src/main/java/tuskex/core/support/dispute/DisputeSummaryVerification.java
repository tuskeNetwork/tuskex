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

package tuskex.core.support.dispute;

import tuskex.common.crypto.Hash;
import tuskex.common.crypto.PubKeyRing;
import tuskex.common.util.Utilities;
import tuskex.core.locale.Res;
import tuskex.core.support.dispute.agent.DisputeAgent;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorManager;
import tuskex.core.trade.TuskexUtils;
import tuskex.network.p2p.NodeAddress;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class DisputeSummaryVerification {
    // Must not change as it is used for splitting the text for verifying the signature of the summary message
    private static final String SEPARATOR1 = "\n-----BEGIN SIGNATURE-----\n";
    private static final String SEPARATOR2 = "\n-----END SIGNATURE-----\n";

    public static String signAndApply(DisputeManager<? extends DisputeList<Dispute>> disputeManager,
                                      DisputeResult disputeResult,
                                      String textToSign) {

        byte[] hash = Hash.getSha256Hash(textToSign);
        KeyPair signatureKeyPair = disputeManager.getSignatureKeyPair();
        String sigAsHex;
        try {
            byte[] signature = TuskexUtils.sign(signatureKeyPair.getPrivate(), hash);
            sigAsHex = Utilities.encodeToHex(signature);
            disputeResult.setArbitratorSignature(signature);
        } catch (Exception e) {
            sigAsHex = "Signing failed";
        }

        return Res.get("disputeSummaryWindow.close.msgWithSig",
                textToSign,
                SEPARATOR1,
                sigAsHex,
                SEPARATOR2);
    }

    public static void verifySignature(String input,
                                         ArbitratorManager arbitratorManager) {
        // get dispute agent
        DisputeAgent disputeAgent = null;
        try {
            String[] parts = input.split(SEPARATOR1);
            String textToSign = parts[0];
            String fullAddress = textToSign.split("\n")[1].split(": ")[1];
            NodeAddress nodeAddress = new NodeAddress(fullAddress);
            disputeAgent = arbitratorManager.getDisputeAgentByNodeAddress(nodeAddress).orElse(null);
            checkNotNull(disputeAgent, "Dispute agent is null");
        } catch (Throwable e) {
            e.printStackTrace();
            throw new IllegalArgumentException(Res.get("support.sigCheck.popup.invalidFormat"));
        }

        // verify signature with pub key ring
        verifySignature(input, disputeAgent.getPubKeyRing());
    }

    public static void verifySignature(String input,
                                         PubKeyRing agentPubKeyRing) {
        try {
            String[] parts = input.split(SEPARATOR1);
            String textToSign = parts[0];
            String sigString = parts[1].split(SEPARATOR2)[0];
            byte[] sig = Utilities.decodeFromHex(sigString);
            byte[] hash = Hash.getSha256Hash(textToSign);
            try {
                TuskexUtils.verifySignature(agentPubKeyRing, hash, sig);
            } catch (Exception e) {
                throw new IllegalArgumentException(Res.get("support.sigCheck.popup.failed"));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new IllegalArgumentException(Res.get("support.sigCheck.popup.invalidFormat"));
        }
    }
}
