package ru.stepanov.selfcontrol.identity;

import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profiles;
    private final AuthenticationFacade auth;

    public ProfileController(ProfileService profiles, AuthenticationFacade auth) {
        this.profiles = profiles;
        this.auth = auth;
    }

    @GetMapping
    User me() {
        return profiles.get(auth.userId());
    }

    @PatchMapping
    User update(@RequestBody UpdateProfileRequest r) {
        return profiles.update(auth.userId(), r);
    }

    @DeleteMapping
    void delete() {
        profiles.delete(auth.userId());
    }

    public record UpdateProfileRequest(String phoneNumber, String firstName, String middleName, String lastName,
                                       String additionalContact) {
    }
}
