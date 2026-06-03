package ru.stepanov.selfcontrol.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Универсальный ответ об ошибке (4xx/5xx) по REST_КОНТРАКТ.yaml.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant timestamp
) {
    public static ErrorResponse of(int status, ErrorCode error, String message, Instant timestamp) {
        return new ErrorResponse(status, error.name(), message, timestamp);
    }
}
