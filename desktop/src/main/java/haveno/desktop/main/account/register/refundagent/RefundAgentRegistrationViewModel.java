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

package tuskex.desktop.main.account.register.refundagent;


import com.google.inject.Inject;
import tuskex.common.crypto.KeyRing;
import tuskex.core.support.dispute.refund.refundagent.RefundAgent;
import tuskex.core.support.dispute.refund.refundagent.RefundAgentManager;
import tuskex.core.user.User;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.main.account.register.AgentRegistrationViewModel;
import tuskex.network.p2p.P2PService;

import java.util.ArrayList;
import java.util.Date;

public class RefundAgentRegistrationViewModel extends AgentRegistrationViewModel<RefundAgent, RefundAgentManager> {

    @Inject
    public RefundAgentRegistrationViewModel(RefundAgentManager arbitratorManager,
                                            User user,
                                            P2PService p2PService,
                                            TskWalletService tskWalletService,
                                            KeyRing keyRing) {
        super(arbitratorManager, user, p2PService, tskWalletService, keyRing);
    }

    @Override
    protected RefundAgent getDisputeAgent(String registrationSignature,
                                          String emailAddress) {
        return new RefundAgent(
                p2PService.getAddress(),
                keyRing.getPubKeyRing(),
                new ArrayList<>(languageCodes),
                new Date().getTime(),
                registrationKey.getPubKey(),
                registrationSignature,
                emailAddress,
                null,
                null
        );
    }

    @Override
    protected RefundAgent getRegisteredDisputeAgentFromUser() {
        return user.getRegisteredRefundAgent();
    }
}
