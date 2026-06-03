package ru.stepanov.selfcontrol.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import ru.stepanov.selfcontrol.identity.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class TokenService {
    public static final int ACCESS_TOKEN_EXPIRES_IN_SECONDS = 1800;

    private final String secret;

    public TokenService(@Value("${app.security.jwt-secret:dev-secret-change-me}") String secret) {
        this.secret = secret;
    }

    public int accessTokenExpiresInSeconds() {
        return ACCESS_TOKEN_EXPIRES_IN_SECONDS;
    }

    public String accessToken(User u) {
        long exp = Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRES_IN_SECONDS).getEpochSecond();
        String payload = u.getUserId() + ":" + u.getRole() + ":" + exp;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }

    public Optional<AuthUser> parse(String token) {
        try {
            String[] p = token.split("\\.");
            if (p.length != 2) return Optional.empty();
            String payload = new String(Base64.getUrlDecoder().decode(p[0]), StandardCharsets.UTF_8);
            if (!sign(payload).equals(p[1])) return Optional.empty();
            String[] parts = payload.split(":");
            if (parts.length != 3) return Optional.empty();
            if (Long.parseLong(parts[2]) < Instant.now().getEpochSecond()) return Optional.empty();
            return Optional.of(new AuthUser(UUID.fromString(parts[0]), UserRole.fromTokenClaim(parts[1])));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public String randomRefresh() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    public String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return new String(Hex.encode(md.digest(raw.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String sign(String p) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return new String(Hex.encode(mac.doFinal(p.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record AuthUser(UUID userId, UserRole role) {
    }
}
