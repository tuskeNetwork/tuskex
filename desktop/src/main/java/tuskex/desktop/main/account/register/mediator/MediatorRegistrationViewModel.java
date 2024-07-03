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

package tuskex.desktop.main.account.register.mediator;

import com.google.inject.Inject;
import tuskex.common.crypto.KeyRing;
import tuskex.core.support.dispute.mediation.mediator.Mediator;
import tuskex.core.support.dispute.mediation.mediator.MediatorManager;
import tuskex.core.user.User;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.desktop.main.account.register.AgentRegistrationViewModel;
import tuskex.network.p2p.P2PService;

import java.util.ArrayList;
import java.util.Date;

class MediatorRegistrationViewModel extends AgentRegistrationViewModel<Mediator, MediatorManager> {

    @Inject
    public MediatorRegistrationViewModel(MediatorManager mediatorManager,
                                         User user,
                                         P2PService p2PService,
                                         TskWalletService tskWalletService,
                                         KeyRing keyRing) {
        super(mediatorManager, user, p2PService, tskWalletService, keyRing);
    }

    @Override
    protected Mediator getDisputeAgent(String registrationSignature,
                                       String emailAddress) {
        return new Mediator(
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
    protected Mediator getRegisteredDisputeAgentFromUser() {
        return user.getRegisteredMediator();
    }
}
