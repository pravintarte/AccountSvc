package com.cs.accountsvc.controller.advice;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.cs.accountsvc.dto.ApiCodes;
import com.cs.accountsvc.dto.ApiErrorResponse;
import com.cs.accountsvc.exception.AccountNotFoundException;
import com.cs.accountsvc.exception.DuplicateTransactionConflictException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converts expected application and validation failures into stable API errors.
 *
 * <p>The advice is the single place where controller, validation, and service
 * exceptions become API response envelopes. This keeps Account Service clients
 * insulated from Java exception types and gives acceptance tests stable
 * application codes to assert.</p>
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Clock clock;

    /**
     * Handles request-body validation failures raised by annotated DTO fields.
     *
     * @param ex Spring validation exception containing field-level errors
     * @return stable bad-request response with validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.warn("Request validation failed: {}", details);
        return error(HttpStatus.BAD_REQUEST, ApiCodes.VALIDATION_ERROR, "Request validation failed.", details);
    }

    /**
     * Handles validation failures raised by path variables or query parameters.
     *
     * @param ex Jakarta validation exception containing constraint violations
     * @return stable bad-request response with validation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        log.warn("Request constraint validation failed: {}", details);
        return error(HttpStatus.BAD_REQUEST, ApiCodes.VALIDATION_ERROR, "Request validation failed.", details);
    }

    /**
     * Handles JSON parse errors and invalid enum/date/number body values.
     *
     * @param ex message conversion exception raised before controller execution
     * @return stable bad-request response for malformed request bodies
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return error(
                HttpStatus.BAD_REQUEST,
                ApiCodes.MALFORMED_REQUEST,
                "Request body is malformed or contains invalid field values.",
                List.of()
        );
    }

    /**
     * Handles type mismatches in request path or query parameters.
     *
     * @param ex type mismatch exception describing the invalid parameter value
     * @return stable bad-request response with the invalid parameter detail
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Request parameter type mismatch name={} value={} requiredType={}",
                ex.getName(), ex.getValue(), ex.getRequiredType());
        return error(
                HttpStatus.BAD_REQUEST,
                ApiCodes.VALIDATION_ERROR,
                "Request path or query parameter has an invalid value.",
                List.of(ex.getName() + ": " + ex.getValue())
        );
    }

    /**
     * Handles missing required request parameters.
     *
     * @param ex missing-parameter exception raised by Spring MVC
     * @return stable bad-request response naming the missing parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter name={} expectedType={}", ex.getParameterName(), ex.getParameterType());
        return error(
                HttpStatus.BAD_REQUEST,
                ApiCodes.VALIDATION_ERROR,
                "Required request parameter is missing.",
                List.of(ex.getParameterName() + ": required parameter is missing")
        );
    }

    /**
     * Handles account detail or balance lookups for accounts with no ledger rows.
     *
     * @param ex domain exception identifying the missing account
     * @return stable not-found response
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Account lookup failed: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ApiCodes.ACCOUNT_NOT_FOUND, ex.getMessage(), List.of());
    }

    /**
     * Handles unsafe idempotency conflicts where an event id is reused with a
     * different transaction payload.
     *
     * @param ex domain exception identifying the conflicting event id
     * @return stable conflict response
     */
    @ExceptionHandler(DuplicateTransactionConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateConflict(DuplicateTransactionConflictException ex) {
        log.warn("Duplicate transaction conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ApiCodes.DUPLICATE_TRANSACTION_CONFLICT, ex.getMessage(), List.of());
    }

    /**
     * Handles unexpected server-side failures that do not have a more specific
     * mapping.
     *
     * @param ex unhandled exception
     * @return stable internal-server-error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled API error", ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiCodes.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the request.",
                List.of()
        );
    }

    /**
     * Builds the standard API error envelope.
     *
     * @param status HTTP status to return
     * @param code stable Account Service application error code
     * @param description human-readable error description
     * @param details optional validation or diagnostic details
     * @return response entity containing the standard error payload
     */
    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String description,
            List<String> details
    ) {
        log.debug(
                "Building API error response httpStatus={} code={} description={} detailCount={}",
                status.value(),
                code,
                description,
                details.size()
        );
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(Instant.now(clock), status.value(), code, description, details));
    }
}
