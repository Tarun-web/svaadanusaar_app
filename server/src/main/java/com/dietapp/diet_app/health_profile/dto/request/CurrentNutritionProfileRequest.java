package com.dietapp.diet_app.health_profile.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * User-reported daily protein intake.
 *
 * <p>Three states are distinguished:</p>
 * <ul>
 *   <li>field omitted from the JSON — existing value is left unchanged</li>
 *   <li>field present with a number — value is stored</li>
 *   <li>field present as {@code null} — stored value is cleared (intake unknown)</li>
 * </ul>
 *
 * <p>There is deliberately no default: {@code 0} g means "eats no protein",
 * which is not the same as "not supplied".</p>
 *
 * <p>Limits are storage limits only (non-negative, {@code NUMERIC(8,2)}),
 * not clinical limits. Clinical plausibility is the calculator's concern.</p>
 */
public class CurrentNutritionProfileRequest {

    @DecimalMin(value = "0.0", message = "Protein intake cannot be negative.")
    @Digits(integer = 6, fraction = 2,
            message = "Protein intake must have at most 6 integer digits and 2 decimal places.")
    private BigDecimal currentProteinGrams;

    /**
     * Set by the setter, which Jackson calls only when the field is present in the JSON.
     * Deliberately has no getter/setter so clients cannot set it directly.
     */
    private boolean currentProteinGramsSupplied;

    /**
     * Only constructor is the implicit no-arg one. A public one-argument
     * constructor would be picked up by Jackson as a creator, bypassing
     * {@link #setCurrentProteinGrams} and hiding whether the field was present.
     */
    public static CurrentNutritionProfileRequest of(BigDecimal currentProteinGrams) {
        CurrentNutritionProfileRequest request = new CurrentNutritionProfileRequest();
        request.setCurrentProteinGrams(currentProteinGrams);
        return request;
    }

    public BigDecimal getCurrentProteinGrams() {
        return currentProteinGrams;
    }

    public void setCurrentProteinGrams(BigDecimal currentProteinGrams) {
        this.currentProteinGrams = currentProteinGrams;
        this.currentProteinGramsSupplied = true;
    }

    /**
     * Whether the caller supplied the field at all (including an explicit null).
     * Not a bean getter, so it is neither serialised nor mapped.
     */
    public boolean suppliesCurrentProteinGrams() {
        return currentProteinGramsSupplied || currentProteinGrams != null;
    }
}
