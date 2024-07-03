package tuskex.desktop.util.validation;

import tuskex.core.locale.Res;
import tuskex.core.payment.validation.BranchIdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchIdValidatorTest {

    @BeforeEach
    public void setup() {
        Locale.setDefault(new Locale("en", "US"));
        Res.setBaseCurrencyCode("TSK");
        Res.setBaseCurrencyName("Monero");
    }

    @Test
    public void testValidationForArgentina() {
        BranchIdValidator validator = new BranchIdValidator("AR");

        assertTrue(validator.validate("0590").isValid);

        assertFalse(validator.validate("05901").isValid);
    }

}
