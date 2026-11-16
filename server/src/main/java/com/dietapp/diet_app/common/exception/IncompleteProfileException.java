package com.dietapp.diet_app.common.exception;

import java.util.List;

/**
 * The caller's own profile lacks data that a calculation requires (for example
 * height). This is a client-correctable condition, unlike other
 * {@link IllegalStateException}s which signal programming errors and stay 500.
 *
 * <p>It extends {@link IllegalStateException} so existing callers and tests
 * that catch the broader type keep working. {@link RestExceptionHandler} maps
 * it to 422 using only the fixed {@link #missingFields()} names, never the
 * exception message.</p>
 */
public class IncompleteProfileException extends IllegalStateException {

    private final List<String> missingFields;

    /**
     * @param message       internal description (logged, never sent to clients)
     * @param missingFields fixed, client-safe names of the missing profile
     *                      fields, e.g. {@code heightCm}
     */
    public IncompleteProfileException(String message, List<String> missingFields) {
        super(message);
        this.missingFields = List.copyOf(missingFields);
    }

    public List<String> missingFields() {
        return missingFields;
    }
}
