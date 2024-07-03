package tuskex.core.payment.validation;

import tuskex.core.locale.Country;
import tuskex.core.locale.CountryUtil;
import tuskex.core.locale.Res;

import java.util.List;
import java.util.Optional;

public class SepaIBANValidator extends IBANValidator {

    @Override
    public ValidationResult validate(String input) {
        ValidationResult result = super.validate(input);

        if (result.isValid) {
            List<Country> sepaCountries = CountryUtil.getAllSepaCountries();
            String ibanCountryCode = input.substring(0, 2).toUpperCase();
            Optional<Country> ibanCountry = sepaCountries
                    .stream()
                    .filter(c -> c.code.equals(ibanCountryCode))
                    .findFirst();

            if (!ibanCountry.isPresent()) {
                return new ValidationResult(false, Res.get("validation.iban.sepaNotSupported"));
            }
        }

        return result;
    }
}
