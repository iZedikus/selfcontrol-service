package ru.stepanov.selfcontrol.identity;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.api.contract.auth.AuthResponse;
import ru.stepanov.selfcontrol.api.contract.auth.LoginRequest;
import ru.stepanov.selfcontrol.api.contract.auth.RefreshRequest;
import ru.stepanov.selfcontrol.api.contract.auth.RegisterRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    AuthResponse login(@RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@RequestBody RefreshRequest request) {
        return service.refresh(request);
    }
}
