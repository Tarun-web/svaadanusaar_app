package com.dietapp.diet_app.auth.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String phone;
    private String name;
    private String email;

}
