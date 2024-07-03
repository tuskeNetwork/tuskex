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

package tuskex.desktop.main.settings.preferences;


import com.google.inject.Inject;
import tuskex.core.locale.LanguageUtil;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorManager;
import tuskex.core.support.dispute.mediation.mediator.MediatorManager;
import tuskex.core.user.Preferences;
import tuskex.desktop.common.model.ActivatableViewModel;

import java.util.stream.Collectors;

public class PreferencesViewModel extends ActivatableViewModel {

    private final ArbitratorManager arbitratorManager;
    private final MediatorManager mediationManager;
    private final Preferences preferences;

    @Inject
    public PreferencesViewModel(Preferences preferences,
                                ArbitratorManager arbitratorManager,
                                MediatorManager mediationManager) {
        this.preferences = preferences;
        this.arbitratorManager = arbitratorManager;
        this.mediationManager = mediationManager;
    }

    boolean needsSupportLanguageWarning() {
        return !arbitratorManager.isAgentAvailableForLanguage(preferences.getUserLanguage()) ||
                !mediationManager.isAgentAvailableForLanguage(preferences.getUserLanguage());
    }

    String getArbitrationLanguages() {
        return arbitratorManager.getObservableMap().values().stream()
                .flatMap(arbitrator -> arbitrator.getLanguageCodes().stream())
                .distinct()
                .map(LanguageUtil::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    public String getMediationLanguages() {
        return mediationManager.getObservableMap().values().stream()
                .flatMap(mediator -> mediator.getLanguageCodes().stream())
                .distinct()
                .map(LanguageUtil::getDisplayName)
                .collect(Collectors.joining(", "));
    }
}
