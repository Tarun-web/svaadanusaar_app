package com.dietapp.diet_app.auth.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {

    @NotBlank
    private String phone;
    @NotBlank
    private String otp;

    public String getPhone() {
        return phone;
    }
}
