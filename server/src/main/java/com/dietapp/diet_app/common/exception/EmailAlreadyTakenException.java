package com.dietapp.diet_app.common.exception;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException() { super(); }
    public EmailAlreadyTakenException(String message) { super(message); }
}

