package com.dietapp.diet_app.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A single field-level validation problem.
 *
 * <p>Deliberately carries no rejected value: that is raw client input and may
 * be a password, OTP, token or other personal data.</p>
 */
@Getter
@AllArgsConstructor
public class ApiError {

    private String field;

    private String message;

}
