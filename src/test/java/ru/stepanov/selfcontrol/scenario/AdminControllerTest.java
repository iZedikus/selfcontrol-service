package ru.stepanov.selfcontrol.scenario;

import org.junit.jupiter.api.Test;
import ru.stepanov.selfcontrol.api.mapper.AdminUserMapper;
import ru.stepanov.selfcontrol.identity.*;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {
    @Test
    void adminUserResponseMapsContractFields() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(new Email("admin@selfcontrol.local"));
        user.setPhoneNumber(new PhoneNumber("+70000000000"));
        user.setPasswordHash(new PasswordHash("secret-hash"));
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(UserRole.Admin);
        user.setStatus(UserStatus.Active);
        user.setCreatedAt(Instant.parse("2026-06-02T00:00:00Z"));

        var response = AdminUserMapper.toResponse(user);

        assertEquals(user.getUserId(), response.userId());
        assertEquals("admin@selfcontrol.local", response.email());
        assertEquals("Admin", response.firstName());
        assertEquals("User", response.lastName());
        assertEquals(Instant.parse("2026-06-02T00:00:00Z"), response.createdAt());
    }
}
