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

package tuskex.desktop.main.support.dispute.agent.mediation;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.common.config.Config;
import tuskex.common.crypto.KeyRing;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.alert.PrivateNotificationManager;
import tuskex.core.support.SupportType;
import tuskex.core.support.dispute.Dispute;
import tuskex.core.support.dispute.DisputeSession;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorManager;
import tuskex.core.support.dispute.mediation.MediationManager;
import tuskex.core.support.dispute.mediation.MediationSession;
import tuskex.core.trade.TradeManager;
import tuskex.core.user.Preferences;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.desktop.common.view.FxmlView;
import tuskex.desktop.main.overlays.windows.ContractWindow;
import tuskex.desktop.main.overlays.windows.DisputeSummaryWindow;
import tuskex.desktop.main.overlays.windows.TradeDetailsWindow;
import tuskex.desktop.main.support.dispute.agent.DisputeAgentView;

@FxmlView
public class MediatorView extends DisputeAgentView {

    @Inject
    public MediatorView(MediationManager mediationManager,
                        KeyRing keyRing,
                        TradeManager tradeManager,
                        @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter formatter,
                        Preferences preferences,
                        DisputeSummaryWindow disputeSummaryWindow,
                        PrivateNotificationManager privateNotificationManager,
                        ContractWindow contractWindow,
                        TradeDetailsWindow tradeDetailsWindow,
                        AccountAgeWitnessService accountAgeWitnessService,
                        ArbitratorManager arbitratorManager,
                        @Named(Config.USE_DEV_PRIVILEGE_KEYS) boolean useDevPrivilegeKeys) {
        super(mediationManager,
                keyRing,
                tradeManager,
                formatter,
                preferences,
                disputeSummaryWindow,
                privateNotificationManager,
                contractWindow,
                tradeDetailsWindow,
                accountAgeWitnessService,
                arbitratorManager,
                useDevPrivilegeKeys);
    }

    @Override
    public void initialize() {
        super.initialize();
        reOpenButton.setVisible(true);
        reOpenButton.setManaged(true);
        setupReOpenDisputeListener();
    }

    @Override
    protected void activate() {
        super.activate();
        activateReOpenDisputeListener();

        // We need to call applyFilteredListPredicate after we called activateReOpenDisputeListener as we use the
        // "open" string by default and it was applied in the super call but the disputes got set in
        // activateReOpenDisputeListener
        applyFilteredListPredicate(filterTextField.getText());
    }

    @Override
    protected void deactivate() {
        super.deactivate();
        deactivateReOpenDisputeListener();
    }

    @Override
    protected SupportType getType() {
        return SupportType.MEDIATION;
    }

    @Override
    protected DisputeSession getConcreteDisputeChatSession(Dispute dispute) {
        return new MediationSession(dispute, disputeManager.isTrader(dispute));
    }

    @Override
    protected void onCloseDispute(Dispute dispute) {
        chatPopup.closeChat();
        disputeSummaryWindow.show(dispute);
    }
}
