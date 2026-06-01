package ru.stepanov.selfcontrol.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stepanov.selfcontrol.banking.BankingAccountLifecycleService;
import ru.stepanov.selfcontrol.scenario.ScenarioService;
import ru.stepanov.selfcontrol.scenario.UserScenarioRepository;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProfileService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final BankingAccountLifecycleService bankingLifecycle;
    private final UserScenarioRepository scenarios;
    private final ScenarioService scenarioService;

    public ProfileService(UserRepository users, RefreshTokenRepository refreshTokens, BankingAccountLifecycleService bankingLifecycle, UserScenarioRepository scenarios, ScenarioService scenarioService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.bankingLifecycle = bankingLifecycle;
        this.scenarios = scenarios;
        this.scenarioService = scenarioService;
    }

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        if (u.getStatus() == UserStatus.Deleted) throw new IllegalStateException("User account is deleted");
        return u;
    }

    @Transactional
    public User update(UUID userId, ProfileController.UpdateProfileRequest r) {
        User u = get(userId);
        if (r.phoneNumber() != null) u.setPhoneNumber(new PhoneNumber(r.phoneNumber()));
        if (r.firstName() != null) u.setFirstName(r.firstName());
        if (r.middleName() != null) u.setMiddleName(r.middleName());
        if (r.lastName() != null) u.setLastName(r.lastName());
        if (r.additionalContact() != null) u.setAdditionalContact(r.additionalContact());
        return users.save(u);
    }

    @Transactional
    public void delete(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        if (u.getStatus() == UserStatus.Deleted) return;

        scenarios.findByUserIdAndActiveTrue(userId)
                .forEach(scenario -> scenarioService.deactivate(userId, scenario.getUserScenarioId(), true));
        bankingLifecycle.unlinkUserBanking(userId);
        refreshTokens.revokeAll(userId);

        Instant now = Instant.now();
        u.setStatus(UserStatus.Deleted);
        u.setDeletedAt(now);
        u.setUpdatedAt(now);
        users.save(u);
    }
}
