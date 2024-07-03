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

package tuskex.core.tsk;

import com.google.inject.Singleton;
import tuskex.common.app.AppModule;
import tuskex.common.config.Config;
import tuskex.core.api.TskConnectionService;
import tuskex.core.tsk.model.EncryptedConnectionList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TskConnectionModule extends AppModule {

    public TskConnectionModule(Config config) {
        super(config);
    }

    @Override
    protected final void configure() {
        bind(EncryptedConnectionList.class).in(Singleton.class);
        bind(TskConnectionService.class).in(Singleton.class);
    }
}
