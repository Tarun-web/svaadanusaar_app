package com.dietapp.diet_app.user.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dietapp.diet_app.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmailOrPhone(String email, String phone);

    boolean existsByEmail(@Email(message = "Invalid email format") String email);

    Optional<User> findByEmailVerificationToken(String token);
}
