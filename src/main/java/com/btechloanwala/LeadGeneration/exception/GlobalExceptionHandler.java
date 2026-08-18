package com.btechloanwala.LeadGeneration.exception;

import com.btechloanwala.LeadGeneration.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central exception handling. Turns every failure into the shared
 * {@link ApiResponse} contract so the frontend never sees stack traces, SQL
 * exceptions, Hibernate internals, or database credentials.
 *
 * <ul>
 *   <li>Bean Validation failures (e.g. bad mobile / false consent): HTTP 400</li>
 *   <li>Unreadable request body: HTTP 400</li>
 *   <li>Business rule violations (invalid loan type / employment / money): HTTP 422</li>
 *   <li>Anything else: HTTP 500 with a generic message</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request.");
        return new ApiResponse(false, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleNotReadable(HttpMessageNotReadableException ex) {
        return new ApiResponse(false, "Invalid request payload.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse handleIllegalArgument(IllegalArgumentException ex) {
        return new ApiResponse(false, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return new ApiResponse(false, "Unable to process your request. Please try again.");
    }
}