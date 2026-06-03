package ru.stepanov.selfcontrol.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;
import ru.stepanov.selfcontrol.api.contract.ErrorResponse;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> apiException(ApiException e) {
        return error(e.getStatus(), e.getErrorCode(), messageOrDefault(e));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String bodyMessage = e.getReason() != null ? e.getReason() : status.getReasonPhrase();
        return error(status, errorCodeForStatus(status), bodyMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException e) {
        if (isUnauthorizedCredentials(e.getMessage())) {
            return error(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, messageOrDefault(e));
        }
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, messageOrDefault(e));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> illegalState(IllegalStateException e) {
        return error(HttpStatus.CONFLICT, ErrorCode.CONFLICT, messageOrDefault(e));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> notReadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Malformed JSON request");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> internal(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Internal error");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, ErrorCode code, String message) {
        ErrorResponse body = ErrorResponse.of(status.value(), code, message, Instant.now());
        return ResponseEntity.status(status).body(body);
    }

    private static ErrorCode errorCodeForStatus(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case CONFLICT -> ErrorCode.CONFLICT;
            case BAD_REQUEST -> ErrorCode.VALIDATION_ERROR;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.VALIDATION_ERROR;
        };
    }

    private static boolean isUnauthorizedCredentials(String message) {
        if (message == null) {
            return false;
        }
        return message.equalsIgnoreCase("Invalid credentials")
                || message.equalsIgnoreCase("Unknown refresh token");
    }

    private static String messageOrDefault(Throwable e) {
        return e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "Request failed";
    }
}
