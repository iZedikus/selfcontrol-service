package ru.stepanov.selfcontrol.api.contract.auth;

/**
 * POST /api/v1/auth/register
 */
public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber
) {
}
