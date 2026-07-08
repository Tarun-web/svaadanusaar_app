package com.dietapp.diet_app.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {

    // Optional name; if provided should not be longer than 100 chars
    @Size(max = 100)
    private String name;

    // Optional email; if provided must be a valid email
    @Email(message = "Invalid email format")
    private String email;
}
