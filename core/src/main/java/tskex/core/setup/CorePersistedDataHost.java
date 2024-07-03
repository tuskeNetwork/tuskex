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

package tuskex.core.setup;

import com.google.inject.Injector;
import tuskex.common.proto.persistable.PersistedDataHost;
import tuskex.core.offer.OpenOfferManager;
import tuskex.core.support.dispute.arbitration.ArbitrationDisputeListService;
import tuskex.core.support.dispute.mediation.MediationDisputeListService;
import tuskex.core.support.dispute.refund.RefundDisputeListService;
import tuskex.core.trade.ClosedTradableManager;
import tuskex.core.trade.TradeManager;
import tuskex.core.trade.failed.FailedTradesManager;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.core.tsk.model.AddressEntryList;
import tuskex.core.tsk.model.EncryptedConnectionList;
import tuskex.core.tsk.model.TskAddressEntryList;
import tuskex.network.p2p.mailbox.IgnoredMailboxService;
import tuskex.network.p2p.mailbox.MailboxMessageService;
import tuskex.network.p2p.peers.PeerManager;
import tuskex.network.p2p.storage.P2PDataStorage;
import tuskex.network.p2p.storage.persistence.RemovedPayloadsService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CorePersistedDataHost {

    // All classes which are persisting objects need to be added here
    public static List<PersistedDataHost> getPersistedDataHosts(Injector injector) {
        List<PersistedDataHost> persistedDataHosts = new ArrayList<>();
        persistedDataHosts.add(injector.getInstance(Preferences.class));
        persistedDataHosts.add(injector.getInstance(User.class));
        persistedDataHosts.add(injector.getInstance(AddressEntryList.class));
        persistedDataHosts.add(injector.getInstance(TskAddressEntryList.class));
        persistedDataHosts.add(injector.getInstance(EncryptedConnectionList.class));
        persistedDataHosts.add(injector.getInstance(OpenOfferManager.class));
        persistedDataHosts.add(injector.getInstance(TradeManager.class));
        persistedDataHosts.add(injector.getInstance(ClosedTradableManager.class));
        persistedDataHosts.add(injector.getInstance(FailedTradesManager.class));
        persistedDataHosts.add(injector.getInstance(ArbitrationDisputeListService.class));
        persistedDataHosts.add(injector.getInstance(MediationDisputeListService.class));
        persistedDataHosts.add(injector.getInstance(RefundDisputeListService.class));
        persistedDataHosts.add(injector.getInstance(P2PDataStorage.class));
        persistedDataHosts.add(injector.getInstance(PeerManager.class));
        persistedDataHosts.add(injector.getInstance(MailboxMessageService.class));
        persistedDataHosts.add(injector.getInstance(IgnoredMailboxService.class));
        persistedDataHosts.add(injector.getInstance(RemovedPayloadsService.class));

        return persistedDataHosts;
    }
}
