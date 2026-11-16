package com.dietapp.diet_app.common.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    static final String OPTIMISTIC_LOCK_MESSAGE =
            "This resource was modified by another request. Please refresh and try again.";

    static final String UNEXPECTED_ERROR_MESSAGE =
            "An unexpected error occurred. Please try again later.";

    static final String VALIDATION_FAILED_MESSAGE = "Validation failed.";

    static final String MALFORMED_REQUEST_MESSAGE =
            "The request body is missing or malformed.";

    static final String METHOD_NOT_ALLOWED_MESSAGE =
            "This HTTP method is not supported for this endpoint.";

    static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE =
            "The request content type is not supported for this endpoint.";

    static final String MISSING_PARAMETER_MESSAGE =
            "A required request parameter is missing.";

    static final String INCOMPLETE_PROFILE_MESSAGE =
            "Your profile is incomplete. Please complete the missing profile fields and try again.";

    static final String INCOMPLETE_PROFILE_FIELD_MESSAGE = "This profile field is required.";


    /**
     * User not found.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                        .message(
                                ex.getMessage() == null
                                        ? "User not found."
                                        : ex.getMessage()
                        )
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }


    /**
     * Resource not found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }


    /**
     * Email already exists.
     */
    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<ErrorResponse> handleEmailConflict(
            EmailAlreadyTakenException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error(HttpStatus.CONFLICT.getReasonPhrase())
                        .message(
                                ex.getMessage() == null
                                        ? "Email already in use."
                                        : ex.getMessage()
                        )
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }


    /**
     * Generic resource conflict.
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(
            ResourceAlreadyExistsException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error(HttpStatus.CONFLICT.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Optimistic-lock failure: the row was changed by another transaction
     * since it was loaded (version mismatch on {@code @Version}).
     *
     * <p>Covers Spring's {@link OptimisticLockingFailureException} (and its
     * subclass {@code ObjectOptimisticLockingFailureException}) as well as the
     * raw JPA {@link OptimisticLockException}. The response is deliberately
     * generic: the exception message can contain entity names, ids and SQL.</p>
     */
    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            Exception ex,
            HttpServletRequest request
    ) {

        log.warn("Optimistic lock conflict on {} {} ({})",
                request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName());

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error(HttpStatus.CONFLICT.getReasonPhrase())
                        .message(OPTIMISTIC_LOCK_MESSAGE)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }




    /**
     * The caller's profile lacks data a calculation needs (e.g. height).
     *
     * <p>422: the request is well-formed and authenticated, but the stored
     * profile cannot be processed until the user completes it. Only the fixed
     * message and the fixed field names are returned - never the exception
     * message. Other {@link IllegalStateException}s are NOT matched (this
     * handler is for the subclass only) and still reach the generic 500.</p>
     */
    @ExceptionHandler(IncompleteProfileException.class)
    public ResponseEntity<ErrorResponse> handleIncompleteProfile(
            IncompleteProfileException ex,
            HttpServletRequest request
    ) {

        log.warn("Incomplete profile on {} {} (missing: {})",
                request.getMethod(), request.getRequestURI(), ex.missingFields());

        List<ApiError> fields = ex.missingFields().stream()
                .map(f -> new ApiError(f, INCOMPLETE_PROFILE_FIELD_MESSAGE))
                .toList();

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.UNPROCESSABLE_CONTENT.value())
                        .error(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase())
                        .message(INCOMPLETE_PROFILE_MESSAGE)
                        .path(request.getRequestURI())
                        .validationErrors(fields)
                        .build();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(response);
    }


    /**
     * Invalid OTP.
     */
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(
            InvalidOtpException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                        .message(
                                ex.getMessage() == null
                                        ? "Invalid OTP."
                                        : ex.getMessage()
                        )
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }


    /**
     * Request validation errors.
     *
     * Example:
     *
     * @NotNull
     * @NotBlank
     * @Min
     * @Max
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<ApiError> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::mapFieldError)
                        .toList();

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(
                                HttpStatus.BAD_REQUEST
                                        .getReasonPhrase()
                        )
                        .message(VALIDATION_FAILED_MESSAGE)
                        .path(request.getRequestURI())
                        .validationErrors(errors)
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }


    /**
     * Bean validation constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse>
    handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        // ex.getMessage() is not used: it concatenates internal property paths
        // (method/entity names) with the constraint messages.
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

        List<ApiError> errors =
                violations == null
                        ? List.of()
                        : violations.stream()
                                .map(this::mapViolation)
                                .toList();

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(
                                HttpStatus.BAD_REQUEST
                                        .getReasonPhrase()
                        )
                        .message(VALIDATION_FAILED_MESSAGE)
                        .path(request.getRequestURI())
                        .validationErrors(errors)
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }


    /**
     * Business exceptions.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(
                                HttpStatus.BAD_REQUEST
                                        .getReasonPhrase()
                        )
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * Malformed or unreadable request body (invalid JSON, wrong field types,
     * missing body). The framework message is never returned: it can quote
     * parts of the payload and name internal classes.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        return clientError(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST_MESSAGE, request);
    }


    /**
     * HTTP method not supported for the endpoint. Keeps the {@code Allow}
     * header required by RFC 9110, which lists only HTTP method names.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {

        Set<HttpMethod> supported = ex.getSupportedHttpMethods();

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (supported != null && !supported.isEmpty()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setAllow(supported);
            builder.headers(headers);
        }

        return builder.body(
                errorBody(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_MESSAGE, request));
    }


    /**
     * Request {@code Content-Type} not supported for the endpoint. Keeps the
     * {@code Accept} header listing the supported media types.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        List<MediaType> supported = ex.getSupportedMediaTypes();
        if (supported != null && !supported.isEmpty()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(supported);
            builder.headers(headers);
        }

        return builder.body(
                errorBody(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE_MESSAGE, request));
    }


    /**
     * Required query/form parameter is missing. Only the parameter name (part
     * of the public API contract) is returned, never the framework message.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message(MISSING_PARAMETER_MESSAGE)
                        .path(request.getRequestURI())
                        .validationErrors(List.of(
                                new ApiError(ex.getParameterName(), "This parameter is required.")))
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * Authorization failures are not "unexpected errors". Re-throw them so the
     * Spring Security filter chain decides the response: 401 (via the
     * authentication entry point) for anonymous callers and 403 for
     * authenticated callers who lack permission. Without this, the catch-all
     * handler below would turn them into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public void rethrowAccessDenied(AccessDeniedException ex) {
        throw ex;
    }





    /**
     * Catch-all handler.
     *
     * This must be the ONLY @ExceptionHandler(Exception.class)
     * in this class.
     */
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleException(
//            Exception ex,
//            HttpServletRequest request
//    ) {
//
//        ErrorResponse response =
//                ErrorResponse.builder()
//                        .timestamp(LocalDateTime.now())
//                        .status(
//                                HttpStatus.INTERNAL_SERVER_ERROR
//                                        .value()
//                        )
//                        .error(
//                                HttpStatus.INTERNAL_SERVER_ERROR
//                                        .getReasonPhrase()
//                        )
//                        .message(
//                                "An unexpected error occurred."
//                        )
//                        .path(request.getRequestURI())
//                        .build();
//
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(response);
//    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Unhandled exception on {} {} ({})",
                request.getMethod(), request.getRequestURI(), ex.getClass().getName(), ex);

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .message(UNEXPECTED_ERROR_MESSAGE)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }


    /**
     * Converts Spring FieldError into our API error DTO.
     *
     * <p>The rejected value is intentionally not copied: it is raw user input
     * and may be a password, OTP, token or personal data.</p>
     */
    private ApiError mapFieldError(
            FieldError error
    ) {

        return new ApiError(
                error.getField(),
                error.getDefaultMessage()
        );
    }

    /**
     * Converts a Bean Validation violation into our API error DTO. Only the
     * last path node (the property name) is exposed, never the full path and
     * never the invalid value.
     */
    private ApiError mapViolation(
            ConstraintViolation<?> violation
    ) {

        String field = null;
        for (Path.Node node : violation.getPropertyPath()) {
            field = node.getName();
        }

        return new ApiError(field, violation.getMessage());
    }

    /**
     * Builds the standard error body for a fixed, safe message.
     */
    private ErrorResponse errorBody(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }

    private ResponseEntity<ErrorResponse> clientError(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(status)
                .body(errorBody(status, message, request));
    }
}