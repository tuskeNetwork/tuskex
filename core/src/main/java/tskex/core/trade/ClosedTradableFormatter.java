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

package tuskex.core.trade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.Res;
import tuskex.core.monetary.CryptoMoney;
import tuskex.core.monetary.TraditionalMoney;
import tuskex.core.monetary.Volume;
import tuskex.core.offer.OpenOffer;
import static tuskex.core.trade.ClosedTradableUtil.castToTrade;
import static tuskex.core.trade.ClosedTradableUtil.getTotalTxFee;
import static tuskex.core.trade.ClosedTradableUtil.getTotalVolumeByCurrency;
import static tuskex.core.trade.ClosedTradableUtil.isTuskexV1Trade;
import static tuskex.core.trade.ClosedTradableUtil.isOpenOffer;
import static tuskex.core.trade.Trade.DisputeState.DISPUTE_CLOSED;
import static tuskex.core.trade.Trade.DisputeState.MEDIATION_CLOSED;
import static tuskex.core.trade.Trade.DisputeState.REFUND_REQUEST_CLOSED;
import tuskex.core.util.FormattingUtils;
import static tuskex.core.util.FormattingUtils.formatPercentagePrice;
import static tuskex.core.util.FormattingUtils.formatToPercentWithSymbol;
import static tuskex.core.util.VolumeUtil.formatVolume;
import static tuskex.core.util.VolumeUtil.formatVolumeWithCode;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Monetary;

@Slf4j
@Singleton
public class ClosedTradableFormatter {
    // Resource bundle i18n keys with Desktop UI specific property names,
    // having "generic-enough" property values to be referenced in the core layer.
    private static final String I18N_KEY_TOTAL_AMOUNT = "closedTradesSummaryWindow.totalAmount.value";
    private static final String I18N_KEY_TOTAL_TX_FEE = "closedTradesSummaryWindow.totalMinerFee.value";
    private static final String I18N_KEY_TOTAL_TRADE_FEE_BTC = "closedTradesSummaryWindow.totalTradeFeeInTsk.value";

    private final ClosedTradableManager closedTradableManager;

    @Inject
    public ClosedTradableFormatter(ClosedTradableManager closedTradableManager) {
        this.closedTradableManager = closedTradableManager;
    }

    public String getAmountAsString(Tradable tradable) {
        return tradable.getOptionalAmount().map(TuskexUtils::formatTsk).orElse("");
    }

    public String getTotalAmountWithVolumeAsString(BigInteger totalTradeAmount, Volume volume) {
        return Res.get(I18N_KEY_TOTAL_AMOUNT,
                TuskexUtils.formatTsk(totalTradeAmount, true),
                formatVolumeWithCode(volume));
    }

    public String getTotalTxFeeAsString(Tradable tradable) {
        return TuskexUtils.formatTsk(getTotalTxFee(tradable));
    }

    public String getTotalTxFeeAsString(BigInteger totalTradeAmount, BigInteger totalTxFee) {
        double percentage = TuskexUtils.divide(totalTxFee, totalTradeAmount);
        return Res.get(I18N_KEY_TOTAL_TX_FEE,
                TuskexUtils.formatTsk(totalTxFee, true),
                formatToPercentWithSymbol(percentage));
    }

    public String getBuyerSecurityDepositAsString(Tradable tradable) {
        if (tradable instanceof Trade) {
            Trade trade = castToTrade(tradable);
            return TuskexUtils.formatTsk(trade.getBuyerSecurityDepositBeforeMiningFee());
        }
        return TuskexUtils.formatTsk(tradable.getOffer().getMaxBuyerSecurityDeposit());
    }

    public String getSellerSecurityDepositAsString(Tradable tradable) {
        if (tradable instanceof Trade) {
            Trade trade = castToTrade(tradable);
            return TuskexUtils.formatTsk(trade.getSellerSecurityDepositBeforeMiningFee());
        }
        return TuskexUtils.formatTsk(tradable.getOffer().getMaxSellerSecurityDeposit());
    }

    public String getTradeFeeAsString(Tradable tradable, boolean appendCode) {
        BigInteger tradeFee = closedTradableManager.getTskTradeFee(tradable);
        return TuskexUtils.formatTsk(tradeFee, appendCode);
    }

    public String getTotalTradeFeeAsString(BigInteger totalTradeAmount, BigInteger totalTradeFee) {
        double percentage = TuskexUtils.divide(totalTradeFee, totalTradeAmount);
        return Res.get(I18N_KEY_TOTAL_TRADE_FEE_BTC,
                TuskexUtils.formatTsk(totalTradeFee, true),
                formatToPercentWithSymbol(percentage));
    }

    public String getPriceDeviationAsString(Tradable tradable) {
        if (tradable.getOffer().isUseMarketBasedPrice()) {
            return formatPercentagePrice(tradable.getOffer().getMarketPriceMarginPct());
        } else {
            return Res.get("shared.na");
        }
    }

    public String getVolumeAsString(Tradable tradable, boolean appendCode) {
        return tradable.getOptionalVolume().map(volume -> formatVolume(volume, appendCode)).orElse("");
    }

    public String getVolumeCurrencyAsString(Tradable tradable) {
        return tradable.getOptionalVolume().map(Volume::getCurrencyCode).orElse("");
    }

    public String getPriceAsString(Tradable tradable) {
        return tradable.getOptionalPrice().map(FormattingUtils::formatPrice).orElse("");
    }

    public Map<String, String> getTotalVolumeByCurrencyAsString(List<Tradable> tradableList) {
        return getTotalVolumeByCurrency(tradableList).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> {
                            String currencyCode = entry.getKey();
                            Monetary monetary;
                            if (CurrencyUtil.isCryptoCurrency(currencyCode)) {
                                monetary = CryptoMoney.valueOf(currencyCode, entry.getValue());
                            } else {
                                monetary = TraditionalMoney.valueOf(currencyCode, entry.getValue());
                            }
                            return formatVolumeWithCode(new Volume(monetary));
                        }
                ));
    }

    public String getStateAsString(Tradable tradable) {
        if (tradable == null) {
            return "";
        }

        if (isTuskexV1Trade(tradable)) {
            Trade trade = castToTrade(tradable);
            if (trade.isCompleted() || trade.isPayoutPublished()) {
                return Res.get("portfolio.closed.completed");
            } else if (trade.getDisputeState() == DISPUTE_CLOSED) {
                return Res.get("portfolio.closed.ticketClosed");
            } else if (trade.getDisputeState() == MEDIATION_CLOSED) {
                return Res.get("portfolio.closed.mediationTicketClosed");
            } else if (trade.getDisputeState() == REFUND_REQUEST_CLOSED) {
                return Res.get("portfolio.closed.ticketClosed");
            } else {
                log.error("That must not happen. We got a pending state but we are in"
                                + " the closed trades list. state={}",
                        trade.getState().name());
                return Res.get("shared.na");
            }
        } else if (isOpenOffer(tradable)) {
            OpenOffer.State state = ((OpenOffer) tradable).getState();
            log.trace("OpenOffer state={}", state);
            switch (state) {
                case AVAILABLE:
                case RESERVED:
                case CLOSED:
                case DEACTIVATED:
                    log.error("Invalid state {}", state);
                    return state.name();
                case CANCELED:
                    return Res.get("portfolio.closed.canceled");
                default:
                    log.error("Unhandled state {}", state);
                    return state.name();
            }
        }
        return Res.get("shared.na");
    }
}
