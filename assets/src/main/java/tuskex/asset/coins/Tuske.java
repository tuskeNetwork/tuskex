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

package tuskex.asset.coins;

import tuskex.asset.CryptoAccountDisclaimer;
import tuskex.asset.Coin;
import tuskex.asset.CryptoNoteAddressValidator;

@CryptoAccountDisclaimer("account.crypto.popup.tsk.msg")
public class Tuske extends Coin {

    public Tuske() {
        super("Tuske", "TSK", new CryptoNoteAddressValidator(356135456L, 8415490592L, 56727581216L));
    }
}
