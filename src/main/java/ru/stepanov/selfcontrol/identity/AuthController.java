package ru.stepanov.selfcontrol.identity;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthService.AuthResponse register(@RequestBody AuthService.RegisterRequest r) {
        return service.register(r);
    }

    @PostMapping("/login")
    AuthService.AuthResponse login(@RequestBody AuthService.LoginRequest r) {
        return service.login(r);
    }

    @PostMapping("/refresh")
    AuthService.AuthResponse refresh(@RequestBody AuthService.RefreshRequest r) {
        return service.refresh(r);
    }

    @PostMapping("/logout")
    void logout(@RequestBody AuthService.RefreshRequest r) {
        service.logout(r);
    }
}
