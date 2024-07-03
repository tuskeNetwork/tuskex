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

import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import tuskex.core.tsk.nodes.TskNodes.TskNode;
import tuskex.network.DnsLookupException;
import tuskex.network.DnsLookupTor;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.net.OnionCatConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

class TskNodeConverter {
    private static final Logger log = LoggerFactory.getLogger(TskNodeConverter.class);

    private final Facade facade;

    TskNodeConverter() {
        this.facade = new Facade();
    }

    TskNodeConverter(Facade facade) {
        this.facade = facade;
    }

    @Nullable
    PeerAddress convertOnionHost(TskNode node) {
        // no DNS lookup for onion addresses
        String onionAddress = Objects.requireNonNull(node.getOnionAddress());
        return new PeerAddress(onionAddress, node.getPort());
    }

    @Nullable
    PeerAddress convertClearNode(TskNode node) {
        int port = node.getPort();

        PeerAddress result = create(node.getHostNameOrAddress(), port);
        if (result == null) {
            String address = node.getAddress();
            if (address != null) {
                result = create(address, port);
            } else {
                log.warn("Lookup failed, no address for node {}", node);
            }
        }
        return result;
    }

    @Nullable
    PeerAddress convertWithTor(TskNode node, Socks5Proxy proxy) {
        int port = node.getPort();

        PeerAddress result = create(proxy, node.getHostNameOrAddress(), port);
        if (result == null) {
            String address = node.getAddress();
            if (address != null) {
                result = create(proxy, address, port);
            } else {
                log.warn("Lookup failed, no address for node {}", node);
            }
        }
        return result;
    }

    @Nullable
    private PeerAddress create(Socks5Proxy proxy, String host, int port) {
        try {
            // We use DnsLookupTor to not leak with DNS lookup
            // Blocking call. takes about 600 ms ;-(
            InetAddress lookupAddress = facade.torLookup(proxy, host);
            InetSocketAddress address = new InetSocketAddress(lookupAddress, port);
            return new PeerAddress(address);
        } catch (Exception e) {
            log.error("Failed to create peer address", e);
            return null;
        }
    }

    @Nullable
    private static PeerAddress create(String hostName, int port) {
        try {
            InetSocketAddress address = new InetSocketAddress(hostName, port);
            return new PeerAddress(address);
        } catch (Exception e) {
            log.error("Failed to create peer address", e);
            return null;
        }
    }

    static class Facade {
        InetAddress onionHostToInetAddress(String onionAddress) throws UnknownHostException {
            return OnionCatConverter.onionHostToInetAddress(onionAddress);
        }

        InetAddress torLookup(Socks5Proxy proxy, String host) throws DnsLookupException {
            return DnsLookupTor.lookup(proxy, host);
        }
    }
}

