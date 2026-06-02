package ru.stepanov.selfcontrol.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.codec.Hex;
import ru.stepanov.selfcontrol.identity.User;
import ru.stepanov.selfcontrol.identity.UserRole;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    private static final String SECRET = "test-secret";
    private final TokenService tokenService = new TokenService(SECRET);

    @Test
    void parseAcceptsCanonicalAdminRoleFromIssuedToken() {
        UUID userId = UUID.randomUUID();
        User admin = new User();
        admin.setUserId(userId);
        admin.setRole(UserRole.Admin);

        var parsed = tokenService.parse(tokenService.accessToken(admin));

        assertTrue(parsed.isPresent());
        assertEquals(userId, parsed.get().userId());
        assertEquals(UserRole.Admin, parsed.get().role());
    }

    @Test
    void parseAcceptsCaseInsensitiveAdminRoleAliases() {
        UUID userId = UUID.randomUUID();

        assertEquals(UserRole.Admin, tokenService.parse(signedToken(userId, "ADMIN")).orElseThrow().role());
        assertEquals(UserRole.Admin, tokenService.parse(signedToken(userId, "admin")).orElseThrow().role());
        assertEquals(UserRole.Admin, tokenService.parse(signedToken(userId, "ROLE_ADMIN")).orElseThrow().role());
    }

    private String signedToken(UUID userId, String role) {
        String payload = userId + ":" + role + ":" + Instant.now().plus(Duration.ofMinutes(30)).getEpochSecond();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return new String(Hex.encode(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
