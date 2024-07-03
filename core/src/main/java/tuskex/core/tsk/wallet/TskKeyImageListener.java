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

package tuskex.core.tsk.wallet;

import monero.daemon.model.MoneroKeyImageSpentStatus;

import java.util.Map;

public interface TskKeyImageListener {

    /**
     * Called with changes to the spent status of key images.
     */
    public void onSpentStatusChanged(Map<String, MoneroKeyImageSpentStatus> spentStatuses);
}
