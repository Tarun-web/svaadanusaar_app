package com.dietapp.diet_app.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiError {

    private String field;

    private Object rejectedValue;

    private String message;

}