package ru.stepanov.selfcontrol.api.contract.auth;

import java.util.UUID;

/**
 * Ответ аутентификации (register / login / refresh) по REST_КОНТРАКТ.yaml.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        int expiresIn,
        UUID userId
) {
}
