package ru.stepanov.selfcontrol.api.contract.admin;

/**
 * Допустимые значения для PATCH /api/v1/admin/users/{userId}/status.
 */
public enum AdminUserStatus {
    Active,
    Blocked
}
