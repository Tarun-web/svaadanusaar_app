package com.dietapp.diet_app.common.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() { super(); }
    public UserNotFoundException(String message) { super(message); }
}

