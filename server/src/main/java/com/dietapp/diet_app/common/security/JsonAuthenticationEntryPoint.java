package com.dietapp.diet_app.common.security;

import com.dietapp.diet_app.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Single, safe response for every unauthenticated request to a protected resource:
 * no token, malformed token, bad signature, expired token or blacklisted token.
 *
 * <p>All of those leave the request without an {@code Authentication} (see {@code JwtFilter}),
 * so they all reach this entry point and receive an identical response. Nothing about
 * <em>why</em> authentication failed is revealed: the exception, the token and the
 * validation outcome are deliberately ignored.</p>
 *
 * <p>The body is the application's standard {@link ErrorResponse}, serialised with the
 * application's own {@link JsonMapper}. No redirect and no login page is involved.</p>
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String MESSAGE = "Authentication is required to access this resource.";

    /** Challenge scheme only; carries no error code or description that could distinguish failures. */
    static final String CHALLENGE = "Bearer";

    private final JsonMapper jsonMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ErrorResponse body =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                        .message(MESSAGE)
                        .path(request.getRequestURI())
                        .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, CHALLENGE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
