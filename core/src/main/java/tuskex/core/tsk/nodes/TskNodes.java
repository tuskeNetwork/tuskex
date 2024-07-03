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

package tuskex.core.tsk.nodes;

import tuskex.common.config.Config;
import tuskex.core.trade.TuskexUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TskNodes {

    // TODO: rename to TskNodeType ?
    public enum MoneroNodesOption {
        PROVIDED,
        CUSTOM,
        PUBLIC
    }

    public List<TskNode> selectPreferredNodes(TskNodesSetupPreferences tskNodesSetupPreferences) {
        return tskNodesSetupPreferences.selectPreferredNodes(this);
    }

    // TODO: always using null hostname
    public List<TskNode> getAllTskNodes() {
        switch (Config.baseCurrencyNetwork()) {
            case TSK_LOCAL:
                return Arrays.asList(
                    new TskNode(MoneroNodesOption.PROVIDED, null, null, "127.0.0.1", 30241, 1, "@local")
                );
            case TSK_STAGENET:
                return Arrays.asList(
                    new TskNode(MoneroNodesOption.PROVIDED, null, null, "127.0.0.1", 40241, 1, "@local"),
                    new TskNode(MoneroNodesOption.PROVIDED, null, null, "127.0.0.1", 40241, 1, "@local")
                );
            case TSK_MAINNET:
                return Arrays.asList(
                    new TskNode(MoneroNodesOption.PROVIDED, null, null, "127.0.0.1", 20241, 1, "@local"),
                    new TskNode(MoneroNodesOption.PUBLIC, null, null, "node-us.tuske.network", 2024, 2, "@tuskeTeam"),
                    new TskNode(MoneroNodesOption.PUBLIC, null, null, "node-jp.tuske.network", 2024, 2, "@tuskeTeam"),
                    new TskNode(MoneroNodesOption.PROVIDED, null, "tuske5hkcseb6srksudc2eiuhkcetf7ibbkz3hsbfbar2kyi2lykkrid.onion",null, 20243, 2, "@tuskeTeam"),
                    new TskNode(MoneroNodesOption.PROVIDED, null, "tusketluwz3igsax57dpwetybs5oacvrfcre6xdwl6cheax3i5wh5uyd.onion",null, 20243, 2, "@tuskeTeam")
                );
            default:
                throw new IllegalStateException("Unexpected base currency network: " + Config.baseCurrencyNetwork());
        }
    }

    public List<TskNode> getProvidedTskNodes() {
        return getTskNodes(MoneroNodesOption.PROVIDED);
    }

    public List<TskNode> getPublicTskNodes() {
        return getTskNodes(MoneroNodesOption.PUBLIC);
    }

    public List<TskNode> getCustomTskNodes() {
        return getTskNodes(MoneroNodesOption.CUSTOM);
    }

    private List<TskNode>  getTskNodes(MoneroNodesOption type) {
        List<TskNode> nodes = new ArrayList<>();
        for (TskNode node : getAllTskNodes()) if (node.getType() == type) nodes.add(node);
        return nodes;
    }

    public static List<TskNodes.TskNode> toCustomTskNodesList(Collection<String> nodes) {
        return nodes.stream()
                .filter(e -> !e.isEmpty())
                .map(TskNodes.TskNode::fromFullAddress)
                .collect(Collectors.toList());
    }

    @EqualsAndHashCode
    @Getter
    public static class TskNode {

        private final MoneroNodesOption type;
        @Nullable
        private final String onionAddress;
        @Nullable
        private final String hostName;
        @Nullable
        private final String operator; // null in case the user provides a list of custom btc nodes
        @Nullable
        private final String address; // IPv4 address
        private int port = TuskexUtils.getDefaultMoneroPort();
        private int priority = 0;

        /**
         * @param fullAddress [IPv4 address:port or onion:port]
         * @return TskNode instance
         */
        public static TskNode fromFullAddress(String fullAddress) {
            String[] parts = fullAddress.split("]");
            checkArgument(parts.length > 0);
            String host = "";
            int port = TuskexUtils.getDefaultMoneroPort();
            if (parts[0].contains("[") && parts[0].contains(":")) {
                // IPv6 address and optional port number
                // address part delimited by square brackets e.g. [2a01:123:456:789::2]:8333
                host = parts[0] + "]";  // keep the square brackets per RFC-2732
                if (parts.length == 2)
                    port = Integer.parseInt(parts[1].replace(":", ""));
            } else if (parts[0].contains(":") && !parts[0].contains(".")) {
                // IPv6 address only; not delimited by square brackets
                host = parts[0];
            } else if (parts[0].contains(".")) {
                // address and an optional port number
                // e.g. 127.0.0.1:8333 or abcdef123xyz.onion:9999
                parts = fullAddress.split(":");
                checkArgument(parts.length > 0);
                host = parts[0];
                if (parts.length == 2)
                    port = Integer.parseInt(parts[1]);
            }

            checkArgument(host.length() > 0, "TskNode address format not recognised");
            return host.contains(".onion") ? new TskNode(MoneroNodesOption.CUSTOM, null, host, null, port, null, null) : new TskNode(MoneroNodesOption.CUSTOM, null, null, host, port, null, null);
        }

        public TskNode(MoneroNodesOption type,
                       @Nullable String hostName,
                       @Nullable String onionAddress,
                       @Nullable String address,
                       int port,
                       Integer priority,
                       @Nullable String operator) {
            this.type = type;
            this.hostName = hostName;
            this.onionAddress = onionAddress;
            this.address = address;
            this.port = port;
            this.priority = priority == null ? 0 : priority;
            this.operator = operator;
        }

        public boolean hasOnionAddress() {
            return onionAddress != null;
        }

        public String getHostNameOrAddress() {
            if (hostName != null)
                return hostName;
            else
                return address;
        }

        public boolean hasClearNetAddress() {
            return hostName != null || address != null;
        }

        @Override
        public String toString() {
            return "onionAddress='" + onionAddress + '\'' +
                    ", hostName='" + hostName + '\'' +
                    ", address='" + address + '\'' +
                    ", port='" + port + '\'' +
                    ", priority='" + priority + '\'' +
                    ", operator='" + operator;
        }
    }
}
