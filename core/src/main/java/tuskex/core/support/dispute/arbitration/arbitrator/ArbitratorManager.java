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

package tuskex.core.support.dispute.arbitration.arbitrator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import tuskex.common.config.Config;
import tuskex.common.crypto.KeyRing;
import tuskex.core.filter.FilterManager;
import tuskex.core.support.dispute.agent.DisputeAgentManager;
import tuskex.core.user.User;
import tuskex.network.p2p.storage.payload.ProtectedStorageEntry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ArbitratorManager extends DisputeAgentManager<Arbitrator> {

    @Inject
    public ArbitratorManager(KeyRing keyRing,
                             ArbitratorService arbitratorService,
                             User user,
                             FilterManager filterManager) {
        super(keyRing, arbitratorService, user, filterManager);
    }

    @Override
    protected List<String> getPubKeyList() {
        switch (Config.baseCurrencyNetwork()) {
        case TSK_LOCAL:
            return List.of(
                   "0280c207a23a361cdab1d6f34ec65d39bbd681da7372358b0404a2a26717829bc5",
                    "02ee9dd4be096ece5ccc7d4dd546302178e0ba3b9720062e7ba68dfacf5bfacb13",
                    "0347fc95e1d445523e909c1717cf1208721d68af945da628a66b9db7c6c9f10b23");
        case TSK_STAGENET:
            return List.of(
                    "0280c207a23a361cdab1d6f34ec65d39bbd681da7372358b0404a2a26717829bc5",
                    "02ee9dd4be096ece5ccc7d4dd546302178e0ba3b9720062e7ba68dfacf5bfacb13",
                    "0347fc95e1d445523e909c1717cf1208721d68af945da628a66b9db7c6c9f10b23");
        case TSK_MAINNET:
            return List.of(
                    "0280c207a23a361cdab1d6f34ec65d39bbd681da7372358b0404a2a26717829bc5",
                    "02ee9dd4be096ece5ccc7d4dd546302178e0ba3b9720062e7ba68dfacf5bfacb13",
                    "0347fc95e1d445523e909c1717cf1208721d68af945da628a66b9db7c6c9f10b23");
        default:
            throw new RuntimeException("Unhandled base currency network: " + Config.baseCurrencyNetwork());
        }
    }

    @Override
    protected boolean isExpectedInstance(ProtectedStorageEntry data) {
        return data.getProtectedStoragePayload() instanceof Arbitrator;
    }

    @Override
    protected void addAcceptedDisputeAgentToUser(Arbitrator disputeAgent) {
        user.addAcceptedArbitrator(disputeAgent);
    }

    @Override
    protected void removeAcceptedDisputeAgentFromUser(ProtectedStorageEntry data) {
        user.removeAcceptedArbitrator((Arbitrator) data.getProtectedStoragePayload());
    }

    @Override
    protected List<Arbitrator> getAcceptedDisputeAgentsFromUser() {
        return user.getAcceptedArbitrators();
    }

    @Override
    protected void clearAcceptedDisputeAgentsAtUser() {
        user.clearAcceptedArbitrators();
    }

    @Override
    protected Arbitrator getRegisteredDisputeAgentFromUser() {
        return user.getRegisteredArbitrator();
    }

    @Override
    protected void setRegisteredDisputeAgentAtUser(Arbitrator disputeAgent) {
        user.setRegisteredArbitrator(disputeAgent);
    }
}
