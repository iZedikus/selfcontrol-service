package ru.stepanov.selfcontrol.api.mapper;

import ru.stepanov.selfcontrol.api.contract.admin.AdminUserResponse;
import ru.stepanov.selfcontrol.api.contract.admin.UserAdminStatus;
import ru.stepanov.selfcontrol.identity.User;
import ru.stepanov.selfcontrol.identity.UserStatus;

public final class AdminUserMapper {

    private AdminUserMapper() {
    }

    public static AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getEmail() == null ? null : user.getEmail().getValue(),
                user.getFirstName(),
                user.getLastName(),
                mapStatus(user.getStatus()),
                user.getCreatedAt()
        );
    }

    private static UserAdminStatus mapStatus(UserStatus status) {
        if (status == null) {
            return UserAdminStatus.Active;
        }
        return switch (status) {
            case PendingVerification -> UserAdminStatus.PendingVerification;
            case Active -> UserAdminStatus.Active;
            case Blocked -> UserAdminStatus.Blocked;
            case Deleted -> UserAdminStatus.Deleted;
        };
    }
}
