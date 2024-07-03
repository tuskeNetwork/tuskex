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

import tuskex.core.locale.Res;
import tuskex.core.monetary.Volume;
import tuskex.core.offer.OfferUtil;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.util.VolumeUtil;
import tuskex.core.util.coin.CoinFormatter;
import tuskex.desktop.util.DisplayUtils;
import tuskex.desktop.util.GUIUtil;

import java.math.BigInteger;
import java.util.Optional;

// Shared utils for ViewModels
public class OfferViewModelUtil {
    public static String getTradeFeeWithFiatEquivalent(OfferUtil offerUtil,
                                                       BigInteger tradeFee,
                                                       CoinFormatter formatter) {

        Optional<Volume> optionalBtcFeeInFiat = offerUtil.getFeeInUserFiatCurrency(tradeFee,
                formatter);

        return DisplayUtils.getFeeWithFiatAmount(tradeFee, optionalBtcFeeInFiat, formatter);
    }

    public static String getTradeFeeWithFiatEquivalentAndPercentage(OfferUtil offerUtil,
                                                                    BigInteger tradeFee,
                                                                    BigInteger tradeAmount,
                                                                    CoinFormatter formatter) {
        String feeAsTsk = TuskexUtils.formatTsk(tradeFee, true);
        String percentage;
        percentage = GUIUtil.getPercentage(tradeFee, tradeAmount) + " " + Res.get("guiUtil.ofTradeAmount");
        return offerUtil.getFeeInUserFiatCurrency(tradeFee,
                formatter)
                .map(VolumeUtil::formatAverageVolumeWithCode)
                .map(feeInFiat -> Res.get("feeOptionWindow.tskFeeWithFiatAndPercentage", feeAsTsk, feeInFiat, percentage))
                .orElseGet(() -> Res.get("feeOptionWindow.tskFeeWithPercentage", feeAsTsk, percentage));
    }
}
