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

package tuskex.core.payment.validation;

import com.google.inject.Inject;
import tuskex.core.locale.Res;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.util.validation.NumberValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

public class TskValidator extends NumberValidator {

    @Nullable
    @Setter
    protected BigInteger minValue;

    @Nullable
    @Setter
    protected BigInteger maxValue;

    @Nullable
    @Setter
    @Getter
    protected BigInteger maxTradeLimit;

    @Inject
    public TskValidator() {
    }

    @Override
    public ValidationResult validate(String input) {
        ValidationResult result = validateIfNotEmpty(input);
        if (result.isValid) {
            input = cleanInput(input);
            result = validateIfNumber(input);
        }

        if (result.isValid) {
            result = result.andValidation(input,
                    this::validateIfNotZero,
                    this::validateIfNotNegative,
                    this::validateIfNotFractionalTskValue,
                    this::validateIfNotExceedsMaxTradeLimit,
                    this::validateIfNotExceedsMaxValue,
                    this::validateIfNotUnderMinValue);
        }

        return result;
    }

    protected ValidationResult validateIfNotFractionalTskValue(String input) {
        try {
            BigDecimal bd = new BigDecimal(input);
            final BigDecimal atomicUnits = bd.movePointRight(TuskexUtils.TSK_SMALLEST_UNIT_EXPONENT);
            if (atomicUnits.scale() > 0)
                return new ValidationResult(false, Res.get("validation.tsk.fraction"));
            else
                return new ValidationResult(true);
        } catch (Throwable t) {
            return new ValidationResult(false, Res.get("validation.invalidInput", t.getMessage()));
        }
    }

    protected ValidationResult validateIfNotExceedsMaxValue(String input) {
        try {
            final BigInteger amount = TuskexUtils.parseTsk(input);
            if (maxValue != null && amount.compareTo(maxValue) > 0)
                return new ValidationResult(false, Res.get("validation.tsk.tooLarge", TuskexUtils.formatTsk(maxValue, true)));
            else
                return new ValidationResult(true);
        } catch (Throwable t) {
            return new ValidationResult(false, Res.get("validation.invalidInput", t.getMessage()));
        }
    }

    protected ValidationResult validateIfNotExceedsMaxTradeLimit(String input) {
        try {
            final BigInteger amount = TuskexUtils.parseTsk(input);
            if (maxTradeLimit != null && amount.compareTo(maxTradeLimit) > 0)
                return new ValidationResult(false, Res.get("validation.tsk.exceedsMaxTradeLimit", TuskexUtils.formatTsk(maxTradeLimit, true)));
            else
                return new ValidationResult(true);
        } catch (Throwable t) {
            return new ValidationResult(false, Res.get("validation.invalidInput", t.getMessage()));
        }
    }

    protected ValidationResult validateIfNotUnderMinValue(String input) {
        try {
            final BigInteger amount = TuskexUtils.parseTsk(input);
            if (minValue != null && amount.compareTo(minValue) < 0)
                return new ValidationResult(false, Res.get("validation.tsk.tooSmall", TuskexUtils.formatTsk(minValue)));
            else
                return new ValidationResult(true);
        } catch (Throwable t) {
            return new ValidationResult(false, Res.get("validation.invalidInput", t.getMessage()));
        }
    }
}
