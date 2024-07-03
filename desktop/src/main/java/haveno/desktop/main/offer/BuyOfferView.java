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

package tuskex.desktop.main.offer;

import com.google.inject.Inject;
import tuskex.core.offer.OfferDirection;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.desktop.Navigation;
import tuskex.desktop.common.view.FxmlView;
import tuskex.desktop.common.view.ViewLoader;
import tuskex.network.p2p.P2PService;

@FxmlView
public class BuyOfferView extends OfferView {

    @Inject
    public BuyOfferView(ViewLoader viewLoader,
                        Navigation navigation,
                        Preferences preferences,
                        User user,
                        P2PService p2PService) {
        super(viewLoader,
                navigation,
                preferences,
                user,
                p2PService,
                OfferDirection.BUY);
    }
}
