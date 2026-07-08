package com.dietapp.diet_app.auth.controller;

import com.dietapp.diet_app.auth.dto.request.OtpVerifyRequest;
import com.dietapp.diet_app.auth.dto.response.LoginResponse;
import com.dietapp.diet_app.auth.service.AuthService;
import com.dietapp.diet_app.common.util.JwtUtil;
import com.dietapp.diet_app.common.service.TokenBlacklistService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private final AuthService authService;
    @Autowired
    private final JwtUtil jwtUtil;
    @Autowired
    private final TokenBlacklistService tokenBlacklistService;

    // verify otp
    @PostMapping("/otp/verify")
    public LoginResponse verifyOtp(@RequestBody @Valid OtpVerifyRequest req){
        return authService.verifyOtp(req.getPhone(), req.getOtp());
    }

    // verify email via token
    @GetMapping("/email/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token){
        authService.verifyEmail(token);
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    // Logout: blacklist the provided token so it cannot be used again until expiry
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(jakarta.servlet.http.HttpServletRequest request){
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                java.util.Date exp = jwtUtil.getClaimsFromToken(token).getExpiration();
                long ttl = exp.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    tokenBlacklistService.blacklistToken(token, ttl);
                }
            }
        }
    }

}
