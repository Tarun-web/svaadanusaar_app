package com.dietapp.diet_app.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String email, String token) {
        String verificationLink = "http://localhost:8080/api/v1/auth/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Email Verification");
        message.setText("Please verify your email by clicking the following link: " + verificationLink);

        mailSender.send(message);
    }
}
