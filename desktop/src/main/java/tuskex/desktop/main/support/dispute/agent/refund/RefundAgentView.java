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

package tuskex.desktop.main.support.dispute.agent.refund;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.common.config.Config;
import tuskex.common.crypto.KeyRing;
import tuskex.core.account.witness.AccountAgeWitnessService;
import tuskex.core.alert.PrivateNotificationManager;
import tuskex.core.locale.Res;
import tuskex.core.support.SupportType;
import tuskex.core.support.dispute.Dispute;
import tuskex.core.support.dispute.DisputeSession;
import tuskex.core.support.dispute.arbitration.arbitrator.ArbitratorManager;
import tuskex.core.support.dispute.refund.RefundManager;
import tuskex.core.support.dispute.refund.RefundSession;
import tuskex.core.trade.TradeManager;
import tuskex.core.user.Preferences;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.desktop.common.view.FxmlView;
import tuskex.desktop.main.overlays.popups.Popup;
import tuskex.desktop.main.overlays.windows.ContractWindow;
import tuskex.desktop.main.overlays.windows.DisputeSummaryWindow;
import tuskex.desktop.main.overlays.windows.TradeDetailsWindow;
import tuskex.desktop.main.support.dispute.agent.DisputeAgentView;

@FxmlView
public class RefundAgentView extends DisputeAgentView {

    @Inject
    public RefundAgentView(RefundManager refundManager,
                           KeyRing keyRing,
                           TradeManager tradeManager,
                           @Named(FormattingUtils.BTC_FORMATTER_KEY) CoinFormatter formatter,
                           Preferences preferences,
                           DisputeSummaryWindow disputeSummaryWindow,
                           PrivateNotificationManager privateNotificationManager,
                           ContractWindow contractWindow,
                           TradeDetailsWindow tradeDetailsWindow,
                           AccountAgeWitnessService accountAgeWitnessService,
                           ArbitratorManager arbitratorService,
                           @Named(Config.USE_DEV_PRIVILEGE_KEYS) boolean useDevPrivilegeKeys) {
        super(refundManager,
                keyRing,
                tradeManager,
                formatter,
                preferences,
                disputeSummaryWindow,
                privateNotificationManager,
                contractWindow,
                tradeDetailsWindow,
                accountAgeWitnessService,
                arbitratorService,
                useDevPrivilegeKeys);
    }

    @Override
    protected SupportType getType() {
        return SupportType.REFUND;
    }

    @Override
    protected DisputeSession getConcreteDisputeChatSession(Dispute dispute) {
        return new RefundSession(dispute, disputeManager.isTrader(dispute));
    }

    @Override
    protected void onCloseDispute(Dispute dispute) {
        long protocolVersion = dispute.getContract().getOfferPayload().getProtocolVersion();
        // Refund agent was introduced with protocolVersion version 2. We do not support old trade protocol cases.
        if (protocolVersion >= 2) {
            chatPopup.closeChat();
            disputeSummaryWindow.show(dispute);
        } else {
            new Popup().warning(Res.get("support.wrongVersion", protocolVersion)).show();
        }
    }
}
