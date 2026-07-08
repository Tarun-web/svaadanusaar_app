package com.dietapp.diet_app.auth.service;

import com.dietapp.diet_app.auth.dto.response.LoginResponse;
import com.dietapp.diet_app.auth.entity.UserAuth;
import com.dietapp.diet_app.auth.repository.UserAuthRepository;
import com.dietapp.diet_app.common.util.JwtUtil;
import com.dietapp.diet_app.user.entity.User;
import com.dietapp.diet_app.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import com.dietapp.diet_app.common.exception.InvalidOtpException;

@Service
@AllArgsConstructor
@Data
public class AuthService {

    @Autowired
    private final JwtUtil jwtUtil;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final UserAuthRepository userAuthRepository;

    // controller to verify OTP and return JWT token
    public LoginResponse verifyOtp(String phone, String otp){
        // Temporary hardcoded OTP
        if (!"123456".equals(otp)) {
            throw new InvalidOtpException("Invalid OTP");
        }
        // Find or create user
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setPhone(phone);
                    return userRepository.save(newUser);
                });

        // Find or create user auth
        UserAuth auth = userAuthRepository.findById(user.getId())
                .orElse(new UserAuth());

        auth.setUser(user);
        auth.setProvider("OTP");
        auth.setProviderId(phone);
        auth.setLastLoginAt(LocalDateTime.now());
        userAuthRepository.save(auth);

        String token = jwtUtil.generateToken(user.getId());
        return new LoginResponse(token, user.getId().toString(), phone);
    }

    // verify email
    public void verifyEmail(String token){

        // fetch the user from token
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidOtpException("Invalid token"));

        // if token is expired
        if(user.getEmailVerificationTokenExpiry().isBefore(Instant.now())){
            throw new InvalidOtpException("Token expired");
        }

        // Save emailVerified as true and delete the token and expiry
        user.setEmailVerified(true);

        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);

        userRepository.save(user);
    }

}
