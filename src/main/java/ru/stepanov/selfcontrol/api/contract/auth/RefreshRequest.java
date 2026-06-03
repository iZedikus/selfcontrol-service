package ru.stepanov.selfcontrol.api.contract.auth;

/**
 * POST /api/v1/auth/refresh
 */
public record RefreshRequest(String refreshToken) {
}
