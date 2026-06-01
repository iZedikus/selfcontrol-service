package ru.stepanov.selfcontrol.identity;

import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final UserRepository users;
    private final AuthenticationFacade auth;

    public ProfileController(UserRepository users, AuthenticationFacade auth) {
        this.users = users;
        this.auth = auth;
    }

    @GetMapping
    User me() {
        return users.findById(auth.userId()).orElseThrow();
    }

    @PatchMapping
    User update(@RequestBody UpdateProfileRequest r) {
        User u = users.findById(auth.userId()).orElseThrow();
        if (r.phoneNumber() != null) u.setPhoneNumber(new PhoneNumber(r.phoneNumber()));
        return users.save(u);
    }

    public record UpdateProfileRequest(String phoneNumber) {
    }
}
