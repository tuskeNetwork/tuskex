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

package tuskex.core.tsk.nodes;

import com.google.common.collect.Lists;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import tuskex.core.tsk.nodes.TskNodes.TskNode;
import org.bitcoinj.core.PeerAddress;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TskNodesRepositoryTest {
    @Test
    public void testGetPeerAddressesWhenClearNodes() {
        TskNode node = mock(TskNode.class);
        when(node.hasClearNetAddress()).thenReturn(true);

        TskNodeConverter converter = mock(TskNodeConverter.class, RETURNS_DEEP_STUBS);
        TskNodesRepository repository = new TskNodesRepository(converter,
                Collections.singletonList(node));

        List<PeerAddress> peers = repository.getPeerAddresses(null, false);

        assertFalse(peers.isEmpty());
    }

    @Test
    public void testGetPeerAddressesWhenConverterReturnsNull() {
        TskNodeConverter converter = mock(TskNodeConverter.class);
        when(converter.convertClearNode(any())).thenReturn(null);

        TskNode node = mock(TskNode.class);
        when(node.hasClearNetAddress()).thenReturn(true);

        TskNodesRepository repository = new TskNodesRepository(converter,
                Collections.singletonList(node));

        List<PeerAddress> peers = repository.getPeerAddresses(null, false);

        verify(converter).convertClearNode(any());
        assertTrue(peers.isEmpty());
    }

    @Test
    public void testGetPeerAddressesWhenProxyAndClearNodes() {
        TskNode node = mock(TskNode.class);
        when(node.hasClearNetAddress()).thenReturn(true);

        TskNode onionNode = mock(TskNode.class);
        when(node.hasOnionAddress()).thenReturn(true);

        TskNodeConverter converter = mock(TskNodeConverter.class, RETURNS_DEEP_STUBS);
        TskNodesRepository repository = new TskNodesRepository(converter,
                Lists.newArrayList(node, onionNode));

        List<PeerAddress> peers = repository.getPeerAddresses(mock(Socks5Proxy.class), true);

        assertEquals(2, peers.size());
    }

    @Test
    public void testGetPeerAddressesWhenOnionNodesOnly() {
        TskNode node = mock(TskNode.class);
        when(node.hasClearNetAddress()).thenReturn(true);

        TskNode onionNode = mock(TskNode.class);
        when(node.hasOnionAddress()).thenReturn(true);

        TskNodeConverter converter = mock(TskNodeConverter.class, RETURNS_DEEP_STUBS);
        TskNodesRepository repository = new TskNodesRepository(converter,
                Lists.newArrayList(node, onionNode));

        List<PeerAddress> peers = repository.getPeerAddresses(mock(Socks5Proxy.class), false);

        assertEquals(1, peers.size());
    }
}
