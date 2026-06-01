package ru.stepanov.selfcontrol.scenario;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.stepanov.selfcontrol.identity.*;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchasePlugin;

import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {
    private final ScenarioTemplateRepository templates;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DataInitializer(ScenarioTemplateRepository templates, UserRepository users, PasswordEncoder encoder) {
        this.templates = templates;
        this.users = users;
        this.encoder = encoder;
    }

    public void run(String... args) {
        templates.findFirstByScenarioTypeCode(UndesirablePurchasePlugin.SCENARIO_TYPE_CODE).orElseGet(() -> {
            ScenarioTemplate t = new ScenarioTemplate();
            t.setScenarioTypeCode(UndesirablePurchasePlugin.SCENARIO_TYPE_CODE);
            t.setName("Нежелательные покупки");
            t.setDescription("Списывает заданную сумму при обнаружении покупки по MCC/merchant rules");
            t.setPublished(true);
            t.setMccCodes(new LinkedHashSet<>(List.of("5813", "5814", "5912", "5921", "5993")));
            return templates.save(t);
        });
        if (!users.existsByEmail_Value("admin@selfcontrol.local")) {
            User u = new User();
            u.setEmail(new Email("admin@selfcontrol.local"));
            u.setPhoneNumber(new PhoneNumber("+70000000000"));
            u.setPasswordHash(new PasswordHash(encoder.encode("admin")));
            u.setRole(UserRole.Admin);
            u.setStatus(UserStatus.Active);
            users.save(u);
        }
    }
}
