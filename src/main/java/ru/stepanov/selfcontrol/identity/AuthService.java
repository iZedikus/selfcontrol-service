package ru.stepanov.selfcontrol.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.security.TokenService;

import java.time.*;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final AuditService audit;

    public AuthService(UserRepository users, RefreshTokenRepository tokens, PasswordEncoder encoder, TokenService tokenService, AuditService audit) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.audit = audit;
    }

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (users.existsByEmail_Value(r.email())) throw new IllegalArgumentException("Email already registered");
        User u = new User();
        u.setEmail(new Email(r.email()));
        u.setPhoneNumber(new PhoneNumber(r.phoneNumber()));
        u.setPasswordHash(new PasswordHash(encoder.encode(r.password())));
        u.setRole(UserRole.User);
        u.setStatus(UserStatus.Active);
        users.save(u);
        audit.record(u.getUserId(), u.getUserId(), "USER_REGISTERED", "USER", u.getUserId(), Map.of("email", r.email()));
        return issue(u);
    }

    @Transactional
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail_Value(r.email()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (u.getStatus() != UserStatus.Active) throw new IllegalStateException("User is not active");
        if (!encoder.matches(r.password(), u.getPasswordHash().getValue()))
            throw new IllegalArgumentException("Invalid credentials");
        u.setLastLoginAt(Instant.now());
        audit.record(u.getUserId(), u.getUserId(), "USER_LOGIN", "USER", u.getUserId(), Map.of("email", r.email()));
        return issue(u);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest r) {
        String hash = tokenService.hash(r.refreshToken());
        RefreshToken old = tokens.findByTokenHash(hash).orElseThrow(() -> new IllegalArgumentException("Unknown refresh token"));
        if (old.isRevoked() || old.getExpiresAt().isBefore(Instant.now()))
            throw new IllegalStateException("Refresh token expired or revoked");
        if (old.getUser().getStatus() != UserStatus.Active) throw new IllegalStateException("User is not active");
        old.setRevoked(true);
        return issue(old.getUser());
    }

    @Transactional
    public void logout(RefreshRequest r) {
        tokens.findByTokenHash(tokenService.hash(r.refreshToken())).ifPresent(t -> {
            t.setRevoked(true);
            tokens.save(t);
        });
    }

    private AuthResponse issue(User u) {
        String raw = tokenService.randomRefresh();
        RefreshToken rt = new RefreshToken();
        rt.setUser(u);
        rt.setTokenHash(tokenService.hash(raw));
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        tokens.save(rt);
        return new AuthResponse(tokenService.accessToken(u), raw, u.getUserId(), u.getRole().name(), u.getStatus().name());
    }

    public record RegisterRequest(String email, String password, String phoneNumber) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, UUID userId, String role, String status) {
    }
}
