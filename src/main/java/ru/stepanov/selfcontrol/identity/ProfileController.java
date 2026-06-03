package ru.stepanov.selfcontrol.identity;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.api.contract.auth.RefreshRequest;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profiles;
    private final AuthService authService;
    private final AuthenticationFacade auth;

    public ProfileController(ProfileService profiles, AuthService authService, AuthenticationFacade auth) {
        this.profiles = profiles;
        this.authService = authService;
        this.auth = auth;
    }

    @GetMapping
    ProfileResponse me() {
        return ProfileResponse.from(profiles.get(auth.userId()));
    }

    @PatchMapping
    ProfileResponse update(@RequestBody UpdateProfileRequest r) {
        return ProfileResponse.from(profiles.update(auth.userId(), r));
    }

    @DeleteMapping
    void delete() {
        profiles.delete(auth.userId());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    public record UpdateProfileRequest(String phoneNumber, String firstName, String middleName, String lastName,
                                       String additionalContact) {
    }

    public record ProfileResponse(UUID userId, String email, String phoneNumber, String firstName, String middleName,
                                  String lastName, String additionalContact, String role, String status,
                                  Instant createdAt, Instant updatedAt, Instant lastLoginAt) {
        static ProfileResponse from(User user) {
            return new ProfileResponse(
                    user.getUserId(),
                    user.getEmail() == null ? null : user.getEmail().getValue(),
                    user.getPhoneNumber() == null ? null : user.getPhoneNumber().getValue(),
                    user.getFirstName(),
                    user.getMiddleName(),
                    user.getLastName(),
                    user.getAdditionalContact(),
                    user.getRole() == null ? null : user.getRole().name(),
                    user.getStatus() == null ? null : user.getStatus().name(),
                    user.getCreatedAt(),
                    user.getUpdatedAt(),
                    user.getLastLoginAt());
        }
    }
}
