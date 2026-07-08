package com.dietapp.diet_app.auth.filter;

import com.dietapp.diet_app.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import com.dietapp.diet_app.common.service.TokenBlacklistService;

@Component
public class JwtFilter extends OncePerRequestFilter {


    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            // Validate token signature/expiry
            if (jwtUtil.validateToken(token)) {
                // Check blacklist
                if (!tokenBlacklistService.isBlacklisted(token)) {
                    UUID userId = jwtUtil.getUserIdFromToken(token);

                    Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
        return;
    }
}