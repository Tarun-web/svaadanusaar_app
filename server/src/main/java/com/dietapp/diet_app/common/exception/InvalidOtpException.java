package com.dietapp.diet_app.common.exception;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException() { super(); }
    public InvalidOtpException(String message) { super(message); }
}

