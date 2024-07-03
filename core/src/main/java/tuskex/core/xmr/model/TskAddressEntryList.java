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

package tuskex.core.tsk.model;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.protobuf.Message;
import tuskex.common.persistence.PersistenceManager;
import tuskex.common.proto.persistable.PersistableEnvelope;
import tuskex.common.proto.persistable.PersistedDataHost;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;



/**
 * The AddressEntries was previously stored as list, now as hashSet. We still keep the old name to reflect the
 * associated protobuf message.
 */
@Slf4j
public final class TskAddressEntryList implements PersistableEnvelope, PersistedDataHost {
    transient private PersistenceManager<TskAddressEntryList> persistenceManager;
    private final Set<TskAddressEntry> entrySet = new CopyOnWriteArraySet<>();

    @Inject
    public TskAddressEntryList(PersistenceManager<TskAddressEntryList> persistenceManager) {
        this.persistenceManager = persistenceManager;

        this.persistenceManager.initialize(this, PersistenceManager.Source.PRIVATE);
    }

    @Override
    public void readPersisted(Runnable completeHandler) {
        persistenceManager.readPersisted(persisted -> {
            entrySet.clear();
            entrySet.addAll(persisted.entrySet);
            completeHandler.run();
        },
        completeHandler);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    private TskAddressEntryList(Set<TskAddressEntry> entrySet) {
        this.entrySet.addAll(entrySet);
    }

    public static TskAddressEntryList fromProto(protobuf.TskAddressEntryList proto) {
        Set<TskAddressEntry> entrySet = proto.getTskAddressEntryList().stream()
                .map(TskAddressEntry::fromProto)
                .collect(Collectors.toSet());
        return new TskAddressEntryList(entrySet);
    }

    @Override
    public Message toProtoMessage() {
        Set<protobuf.TskAddressEntry> addressEntries = entrySet.stream()
                .map(TskAddressEntry::toProtoMessage)
                .collect(Collectors.toSet());
        return protobuf.PersistableEnvelope.newBuilder()
                .setTskAddressEntryList(protobuf.TskAddressEntryList.newBuilder()
                        .addAllTskAddressEntry(addressEntries))
                .build();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public ImmutableList<TskAddressEntry> getAddressEntriesAsListImmutable() {
        return ImmutableList.copyOf(entrySet);
    }

    public boolean addAddressEntry(TskAddressEntry addressEntry) {
        boolean entryWithSameOfferIdAndContextAlreadyExist = entrySet.stream().anyMatch(e -> {
            if (addressEntry.getOfferId() != null) {
                return addressEntry.getOfferId().equals(e.getOfferId()) && addressEntry.getContext() == e.getContext();
            }
            return false;
        });
        if (entryWithSameOfferIdAndContextAlreadyExist) {
            throw new IllegalArgumentException("We have an address entry with the same offer ID and context. We do not add the new one. addressEntry=" + addressEntry);
        }

        boolean setChangedByAdd = entrySet.add(addressEntry);
        if (setChangedByAdd) requestPersistence();
        return setChangedByAdd;
    }

    public void swapToAvailable(TskAddressEntry addressEntry) {
        boolean setChangedByRemove = entrySet.remove(addressEntry);
        boolean setChangedByAdd = entrySet.add(new TskAddressEntry(addressEntry.getSubaddressIndex(), addressEntry.getAddressString(),
                TskAddressEntry.Context.AVAILABLE));
        if (setChangedByRemove || setChangedByAdd) {
            requestPersistence();
        }
    }

    public TskAddressEntry swapAvailableToAddressEntryWithOfferId(TskAddressEntry addressEntry,
                                                               TskAddressEntry.Context context,
                                                               String offerId) {
        // remove old entry
        boolean setChangedByRemove = entrySet.remove(addressEntry);

        // add new entry
        final TskAddressEntry newAddressEntry = new TskAddressEntry(addressEntry.getSubaddressIndex(), addressEntry.getAddressString(), context, offerId, null);
        boolean setChangedByAdd = false;
        try {
            setChangedByAdd = addAddressEntry(newAddressEntry);
        } catch (Exception e) {
            entrySet.add(addressEntry); // undo change if error
            throw e;
        }
        
        if (setChangedByRemove || setChangedByAdd)
            requestPersistence();

        return newAddressEntry;
    }

    public void clear() {
        entrySet.clear();
        requestPersistence();
    }

    public void requestPersistence() {
        persistenceManager.requestPersistence();
    }

    @Override
    public String toString() {
        return "TskAddressEntryList{" +
                ",\n     entrySet=" + entrySet +
                "\n}";
    }
}
