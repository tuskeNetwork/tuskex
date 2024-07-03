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

import tuskex.common.config.Config;
import tuskex.common.util.Utilities;
import tuskex.core.user.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;


public class TskNodesSetupPreferences {
    private static final Logger log = LoggerFactory.getLogger(TskNodesSetupPreferences.class);

    private final Preferences preferences;

    public TskNodesSetupPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public List<TskNodes.TskNode> selectPreferredNodes(TskNodes nodes) {
        List<TskNodes.TskNode> result;

        TskNodes.MoneroNodesOption nodesOption = TskNodes.MoneroNodesOption.values()[preferences.getMoneroNodesOptionOrdinal()];
        switch (nodesOption) {
            case CUSTOM:
                String moneroNodes = preferences.getMoneroNodes();
                Set<String> distinctNodes = Utilities.commaSeparatedListToSet(moneroNodes, false);
                result = TskNodes.toCustomTskNodesList(distinctNodes);
                if (result.isEmpty()) {
                    log.warn("Custom nodes is set but no valid nodes are provided. " +
                            "We fall back to provided nodes option.");
                    preferences.setMoneroNodesOptionOrdinal(TskNodes.MoneroNodesOption.PROVIDED.ordinal());
                    result = nodes.getProvidedTskNodes();
                }
                break;
            case PUBLIC:
                result = nodes.getAllTskNodes(); // public entails provided nodes
                break;
            case PROVIDED:
            default:
                result = nodes.getProvidedTskNodes();
                break;
        }

        return result;
    }

    public boolean isUseCustomNodes() {
        return TskNodes.MoneroNodesOption.CUSTOM.ordinal() == preferences.getMoneroNodesOptionOrdinal();
    }

    public int calculateMinBroadcastConnections(List<TskNodes.TskNode> nodes) {
        TskNodes.MoneroNodesOption nodesOption = TskNodes.MoneroNodesOption.values()[preferences.getMoneroNodesOptionOrdinal()];
        int result;
        switch (nodesOption) {
            case CUSTOM:
                // We have set the nodes already above
                result = (int) Math.ceil(nodes.size() * 0.5);
                // If Tor is set we usually only use onion nodes,
                // but if user provides mixed clear net and onion nodes we want to use both
                break;
            case PUBLIC:
                // We keep the empty nodes
                result = (int) Math.floor(Config.DEFAULT_NUM_CONNECTIONS_FOR_BTC * 0.8);
                break;
            case PROVIDED:
            default:
                // We require only 4 nodes instead of 7 (for 9 max connections) because our provided nodes
                // are more reliable than random public nodes.
                result = 4;
                break;
        }
        return result;
    }

}
