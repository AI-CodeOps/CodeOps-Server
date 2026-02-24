package com.codeops.config;

import com.codeops.dto.response.ErrorResponse;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.CodeOpsException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST controllers in the CodeOps API.
 *
 * <p>Catches application-specific exceptions ({@link NotFoundException}, {@link ValidationException},
 * {@link AuthorizationException}, {@link CodeOpsException}), Spring/JPA exceptions
 * ({@link EntityNotFoundException}, {@link AccessDeniedException}, {@link MethodArgumentNotValidException}),
 * and general uncaught exceptions. Each handler returns a structured {@link ErrorResponse} with the
 * appropriate HTTP status code.</p>
 *
 * <p>Internal error details are never exposed to clients. Application errors ({@link CodeOpsException})
 * and unhandled exceptions are logged at ERROR level with full stack traces. Bad requests from
 * {@link IllegalArgumentException} are logged at WARN level.</p>
 *
 * @see com.codeops.dto.response.ErrorResponse
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles JPA {@link EntityNotFoundException} by returning a 404 response with a generic
     * "Resource not found" message.
     *
     * @param ex the thrown entity not found exception
     * @return a 404 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorResponse(404, "Resource not found"));
    }

    /**
     * Handles {@link IllegalArgumentException} by returning a 400 response with a generic
     * "Invalid request" message. Logs the exception message at WARN level.
     *
     * @param ex the thrown illegal argument exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new ErrorResponse(400, "Invalid request"));
    }

    /**
     * Handles Spring Security {@link AccessDeniedException} by returning a 403 response
     * with an "Access denied" message.
     *
     * @param ex the thrown access denied exception
     * @return a 403 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(403).body(new ErrorResponse(403, "Access denied"));
    }

    /**
     * Handles Jakarta Bean Validation failures by returning a 400 response with a comma-separated
     * list of field-level validation error messages (e.g., {@code "email: must not be blank, name: size must be between 1 and 100"}).
     *
     * @param ex the thrown method argument not valid exception containing binding result errors
     * @return a 400 response with an {@link ErrorResponse} body containing all field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", msg);
        return ResponseEntity.status(400).body(new ErrorResponse(400, msg));
    }

    /**
     * Handles CodeOps-specific {@link NotFoundException} by returning a 404 response with
     * the exception's message as the error detail.
     *
     * @param ex the thrown CodeOps not found exception
     * @return a 404 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCodeOpsNotFound(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorResponse(404, ex.getMessage()));
    }

    /**
     * Handles CodeOps-specific {@link ValidationException} by returning a 400 response with
     * the exception's message as the error detail.
     *
     * @param ex the thrown CodeOps validation exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleCodeOpsValidation(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new ErrorResponse(400, ex.getMessage()));
    }

    /**
     * Handles CodeOps-specific {@link AuthorizationException} by returning a 403 response with
     * the exception's message as the error detail.
     *
     * @param ex the thrown CodeOps authorization exception
     * @return a 403 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleCodeOpsAuth(AuthorizationException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());
        return ResponseEntity.status(403).body(new ErrorResponse(403, ex.getMessage()));
    }

    /**
     * Handles missing required query parameters by returning a 400 response with a descriptive
     * message indicating which parameter is missing.
     *
     * @param ex the thrown missing servlet request parameter exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity.status(400).body(new ErrorResponse(400, "Missing required parameter: " + ex.getParameterName()));
    }

    /**
     * Handles missing required request headers by returning a 400 response.
     *
     * @param ex the thrown missing request header exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing request header: {}", ex.getHeaderName());
        return ResponseEntity.status(400).body(new ErrorResponse(400, "Missing required header: " + ex.getHeaderName()));
    }

    /**
     * Handles type mismatch errors for request parameters (e.g., invalid UUID format).
     *
     * @param ex the thrown method argument type mismatch exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());
        return ResponseEntity.status(400).body(new ErrorResponse(400, "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue()));
    }

    /**
     * Handles unsupported HTTP methods by returning a 405 response.
     *
     * @param ex the thrown HTTP request method not supported exception
     * @return a 405 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());
        return ResponseEntity.status(405).body(new ErrorResponse(405, "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint"));
    }

    /**
     * Handles malformed JSON or type-mismatch errors in request bodies by returning a 400
     * response. Common causes include invalid enum values or unparseable date/time strings.
     *
     * @param ex the thrown HTTP message not readable exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new ErrorResponse(400, "Malformed request body"));
    }

    /**
     * Handles requests for unmapped paths that fall through to the static resource handler.
     *
     * @param ex the thrown no resource found exception
     * @return a 404 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorResponse(404, "Resource not found"));
    }

    /**
     * Handles the base {@link CodeOpsException} by returning a 500 response with a generic
     * error message. Logs the exception at ERROR level with the full stack trace.
     *
     * @param ex the thrown CodeOps application exception
     * @return a 500 response with an {@link ErrorResponse} body (internal details not exposed)
     */
    @ExceptionHandler(CodeOpsException.class)
    public ResponseEntity<ErrorResponse> handleCodeOps(CodeOpsException ex) {
        log.error("Application exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(500).body(new ErrorResponse(500, "An internal error occurred"));
    }

    /**
     * Catch-all handler for any unhandled exceptions not matched by more specific handlers.
     * Returns a 500 response with a generic error message. Logs the exception at ERROR level
     * with the full stack trace for diagnostics.
     *
     * @param ex the unhandled exception
     * @return a 500 response with an {@link ErrorResponse} body (internal details not exposed)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500).body(new ErrorResponse(500, "An internal error occurred"));
    }
}
