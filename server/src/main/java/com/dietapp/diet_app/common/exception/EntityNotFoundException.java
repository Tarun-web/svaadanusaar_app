package com.dietapp.diet_app.common.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException() { super(); }
    public EntityNotFoundException(String message) {
        super(message);
    }
}
