package com.dietapp.diet_app.user.controller;

import com.dietapp.diet_app.auth.dto.response.UserProfileResponse;
import com.dietapp.diet_app.user.dto.UpdateUserRequest;
import com.dietapp.diet_app.user.repository.UserRepository;
import com.dietapp.diet_app.user.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private final UserService userService;

    // Get current user profile
    @GetMapping("/me")
    public UserProfileResponse me(Authentication auth){
        UUID userId = (UUID) auth.getPrincipal();
        assert userId != null; // should never be null here
        return userService.getUserProfile(userId.toString());
    }

    // update user name and email -> logic should be given in frontend in case of otp login
    // otp login -> put("/me")
    // google login -> get("/me") directly as name and email will be fetched from gmail itself
    @PutMapping("/me")
    public UserProfileResponse me(@RequestBody @Valid UpdateUserRequest req, Authentication auth){
        UUID userId = (UUID) auth.getPrincipal();
        assert userId != null; // should never be null here
        return userService.updateUserProfile(userId.toString(), req);
    }

//    Frontend Flow (How it works)
//
//    After login:
//
//    Call:
//
//    GET /api/v1/users/me
//
//
//    If response contains:
//
//    {
//        "name": null,
//            "email": null
//    }
//
//
//→ Show “Complete Profile” screen
//→ Ask for Name & Email
//→ Submit:
//
//    PUT /api/v1/users/me
//    {
//        "name": "Tarun",
//            "email": "tarun@gmail.com"
//    }
//
//
//    Backend updates users table.
//
//    From now on:
//
//    OTP users behave like Google users
//
///users/me always returns full identity
//
//    No special-case logic anywhere else
}
