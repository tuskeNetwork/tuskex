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

package tuskex.core.proto.persistable;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import tuskex.common.proto.ProtobufferRuntimeException;
import tuskex.common.proto.network.NetworkProtoResolver;
import tuskex.common.proto.persistable.NavigationPath;
import tuskex.common.proto.persistable.PersistableEnvelope;
import tuskex.common.proto.persistable.PersistenceProtoResolver;
import tuskex.core.account.sign.SignedWitnessStore;
import tuskex.core.account.witness.AccountAgeWitnessStore;
import tuskex.core.offer.SignedOfferList;
import tuskex.core.payment.PaymentAccountList;
import tuskex.core.proto.CoreProtoResolver;
import tuskex.core.support.dispute.arbitration.ArbitrationDisputeList;
import tuskex.core.support.dispute.mediation.MediationDisputeList;
import tuskex.core.support.dispute.refund.RefundDisputeList;
import tuskex.core.trade.TradableList;
import tuskex.core.trade.statistics.TradeStatistics3Store;
import tuskex.core.user.PreferencesPayload;
import tuskex.core.user.UserPayload;
import tuskex.core.tsk.model.AddressEntryList;
import tuskex.core.tsk.model.EncryptedConnectionList;
import tuskex.core.tsk.model.TskAddressEntryList;
import tuskex.core.tsk.wallet.BtcWalletService;
import tuskex.core.tsk.wallet.TskWalletService;
import tuskex.network.p2p.mailbox.IgnoredMailboxMap;
import tuskex.network.p2p.mailbox.MailboxMessageList;
import tuskex.network.p2p.peers.peerexchange.PeerList;
import tuskex.network.p2p.storage.persistence.RemovedPayloadsMap;
import tuskex.network.p2p.storage.persistence.SequenceNumberMap;
import lombok.extern.slf4j.Slf4j;

// TODO Use ProtobufferException instead of ProtobufferRuntimeException
@Slf4j
@Singleton
public class CorePersistenceProtoResolver extends CoreProtoResolver implements PersistenceProtoResolver {
    private final Provider<BtcWalletService> btcWalletService;
    private final Provider<TskWalletService> tskWalletService;
    private final NetworkProtoResolver networkProtoResolver;

    @Inject
    public CorePersistenceProtoResolver(Provider<BtcWalletService> btcWalletService,
                                        Provider<TskWalletService> tskWalletService,
                                        NetworkProtoResolver networkProtoResolver) {
        this.btcWalletService = btcWalletService;
        this.tskWalletService = tskWalletService;
        this.networkProtoResolver = networkProtoResolver;
    }

    @Override
    public PersistableEnvelope fromProto(protobuf.PersistableEnvelope proto) {
        if (proto != null) {
            switch (proto.getMessageCase()) {
                case SIGNED_OFFER_LIST:
                    return SignedOfferList.fromProto(proto.getSignedOfferList());
                case SEQUENCE_NUMBER_MAP:
                    return SequenceNumberMap.fromProto(proto.getSequenceNumberMap());
                case PEER_LIST:
                    return PeerList.fromProto(proto.getPeerList());
                case ADDRESS_ENTRY_LIST:
                    return AddressEntryList.fromProto(proto.getAddressEntryList());
                case TSK_ADDRESS_ENTRY_LIST:
                    return TskAddressEntryList.fromProto(proto.getTskAddressEntryList());
                case ENCRYPTED_CONNECTION_LIST:
                    return EncryptedConnectionList.fromProto(proto.getEncryptedConnectionList());
                case TRADABLE_LIST:
                    return TradableList.fromProto(proto.getTradableList(), this, tskWalletService.get());
                case ARBITRATION_DISPUTE_LIST:
                    return ArbitrationDisputeList.fromProto(proto.getArbitrationDisputeList(), this);
                case MEDIATION_DISPUTE_LIST:
                    return MediationDisputeList.fromProto(proto.getMediationDisputeList(), this);
                case REFUND_DISPUTE_LIST:
                    return RefundDisputeList.fromProto(proto.getRefundDisputeList(), this);
                case PREFERENCES_PAYLOAD:
                    return PreferencesPayload.fromProto(proto.getPreferencesPayload(), this);
                case USER_PAYLOAD:
                    return UserPayload.fromProto(proto.getUserPayload(), this);
                case NAVIGATION_PATH:
                    return NavigationPath.fromProto(proto.getNavigationPath());
                case PAYMENT_ACCOUNT_LIST:
                    return PaymentAccountList.fromProto(proto.getPaymentAccountList(), this);
                case ACCOUNT_AGE_WITNESS_STORE:
                    return AccountAgeWitnessStore.fromProto(proto.getAccountAgeWitnessStore());
                case SIGNED_WITNESS_STORE:
                    return SignedWitnessStore.fromProto(proto.getSignedWitnessStore());
                case TRADE_STATISTICS3_STORE:
                    return TradeStatistics3Store.fromProto(proto.getTradeStatistics3Store());
                case MAILBOX_MESSAGE_LIST:
                    return MailboxMessageList.fromProto(proto.getMailboxMessageList(), networkProtoResolver);
                case IGNORED_MAILBOX_MAP:
                    return IgnoredMailboxMap.fromProto(proto.getIgnoredMailboxMap());
                case REMOVED_PAYLOADS_MAP:
                    return RemovedPayloadsMap.fromProto(proto.getRemovedPayloadsMap());
                default:
                    throw new ProtobufferRuntimeException("Unknown proto message case(PB.PersistableEnvelope). " +
                            "messageCase=" + proto.getMessageCase() + "; proto raw data=" + proto.toString());
            }
        } else {
            log.error("PersistableEnvelope.fromProto: PB.PersistableEnvelope is null");
            throw new ProtobufferRuntimeException("PB.PersistableEnvelope is null");
        }
    }
}
