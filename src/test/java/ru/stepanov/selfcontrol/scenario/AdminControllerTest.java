package ru.stepanov.selfcontrol.scenario;

import org.junit.jupiter.api.Test;
import ru.stepanov.selfcontrol.identity.*;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {
    @Test
    void adminUserResponseFlattensUserFieldsWithoutPasswordHashOrRefreshTokens() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(new Email("admin@selfcontrol.local"));
        user.setPhoneNumber(new PhoneNumber("+70000000000"));
        user.setPasswordHash(new PasswordHash("secret-hash"));
        user.setRole(UserRole.Admin);
        user.setStatus(UserStatus.Active);
        user.setCreatedAt(Instant.parse("2026-06-02T00:00:00Z"));

        AdminController.AdminUserResponse response = AdminController.AdminUserResponse.from(user);

        assertEquals(user.getUserId(), response.userId());
        assertEquals("admin@selfcontrol.local", response.email());
        assertEquals("+70000000000", response.phoneNumber());
        assertEquals("Admin", response.role());
        assertEquals("Active", response.status());
        assertEquals(Instant.parse("2026-06-02T00:00:00Z"), response.createdAt());
    }
}
