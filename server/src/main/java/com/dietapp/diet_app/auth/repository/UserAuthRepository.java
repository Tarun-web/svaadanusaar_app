package com.dietapp.diet_app.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dietapp.diet_app.auth.entity.UserAuth;

@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, UUID> {
}
