package com.dietapp.diet_app.user.service;

import com.dietapp.diet_app.auth.dto.response.UserProfileResponse;
import com.dietapp.diet_app.notification.service.EmailService;
import com.dietapp.diet_app.user.dto.UpdateUserRequest;
import com.dietapp.diet_app.user.entity.User;
import com.dietapp.diet_app.user.repository.UserRepository;
import com.dietapp.diet_app.common.exception.EmailAlreadyTakenException;
import com.dietapp.diet_app.common.exception.UserNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private EmailService  emailService;

    // Get user profile by user ID
    public UserProfileResponse getUserProfile(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(user -> new UserProfileResponse(
                        user.getId(),
                        user.getPhone(),
                        user.getName(),
                        user.getEmail()
                ))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // update user profile
    public UserProfileResponse updateUserProfile(String userId, UpdateUserRequest req){
        UUID uid = UUID.fromString(userId);

        // fetch user based on id
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // set username
        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName());
        }

        // if no user exists with same email or the email is not already for current user as well
        if (req.getEmail() != null
                && !req.getEmail().equals(user.getEmail())
                && !req.getEmail().isBlank()) {

            // store new Email
            String newEmail = req.getEmail();

            // if not already in user
            userRepository.findByEmail(newEmail)
                    .filter(u -> !u.getId().equals(uid))
                    .ifPresent(u -> { throw new EmailAlreadyTakenException("Email already in use"); });

            // set email and verified(as false)
            user.setEmail(newEmail);
            user.setEmailVerified(false);

            String token = UUID.randomUUID().toString();

            // set token and expiry
            user.setEmailVerificationToken(token);
            user.setEmailVerificationTokenExpiry(Instant.now().plusSeconds(15 * 60)); // 15 minutes expiry

            // call the emailService in notification to send email
            emailService.sendVerificationEmail(newEmail, token);
        }

        // Save to update the user entity in database
        userRepository.save(user);
        return toResponse(user);
    }

    // Convert entity to response DTO
    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getEmail()
        );
    }
}
