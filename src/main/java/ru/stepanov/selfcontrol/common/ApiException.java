package ru.stepanov.selfcontrol.common;

import org.springframework.http.HttpStatus;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;

/**
 * Бизнес-ошибка с HTTP-статусом и кодом из REST_КОНТРАКТ.yaml.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    public ApiException(HttpStatus status, ErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
