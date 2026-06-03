package ru.stepanov.selfcontrol.api.contract.admin;

/**
 * PATCH /api/v1/admin/users/{userId}/status
 */
public record AdminUpdateUserStatusRequest(AdminUserStatus status) {
}
