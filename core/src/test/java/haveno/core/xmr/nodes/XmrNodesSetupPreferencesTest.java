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

import tuskex.core.user.Preferences;
import tuskex.core.tsk.nodes.TskNodes.TskNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static tuskex.core.tsk.nodes.TskNodes.MoneroNodesOption.CUSTOM;
import static tuskex.core.tsk.nodes.TskNodes.MoneroNodesOption.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TskNodesSetupPreferencesTest {
    @Test
    public void testSelectPreferredNodesWhenPublicOption() {
        Preferences delegate = mock(Preferences.class);
        when(delegate.getMoneroNodesOptionOrdinal()).thenReturn(PUBLIC.ordinal());

        TskNodesSetupPreferences preferences = new TskNodesSetupPreferences(delegate);
        List<TskNode> nodes = preferences.selectPreferredNodes(mock(TskNodes.class));

        assertTrue(nodes.isEmpty());
    }

    @Test
    public void testSelectPreferredNodesWhenCustomOption() {
        Preferences delegate = mock(Preferences.class);
        when(delegate.getMoneroNodesOptionOrdinal()).thenReturn(CUSTOM.ordinal());
        when(delegate.getMoneroNodes()).thenReturn("aaa.onion,bbb.onion");

        TskNodesSetupPreferences preferences = new TskNodesSetupPreferences(delegate);
        List<TskNode> nodes = preferences.selectPreferredNodes(mock(TskNodes.class));

        assertEquals(2, nodes.size());
    }
}
