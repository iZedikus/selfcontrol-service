package ru.stepanov.selfcontrol.api.contract.auth;

/**
 * POST /api/v1/auth/login
 */
public record LoginRequest(String email, String password) {
}
