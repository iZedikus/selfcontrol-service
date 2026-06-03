package ru.stepanov.selfcontrol.api.contract.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;
import java.util.UUID;

/**
 * Пользователь в списке GET /api/v1/admin/users.
 */
public record AdminUserResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UserAdminStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant createdAt
) {
}
