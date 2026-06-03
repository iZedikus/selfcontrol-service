package ru.stepanov.selfcontrol.api.v1;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.PagedResponse;
import ru.stepanov.selfcontrol.api.contract.PageUtils;
import ru.stepanov.selfcontrol.api.contract.admin.AdminCreateScenarioTemplateRequest;
import ru.stepanov.selfcontrol.api.contract.admin.AdminUpdateUserStatusRequest;
import ru.stepanov.selfcontrol.api.contract.admin.AdminUserStatus;
import ru.stepanov.selfcontrol.api.contract.admin.AdminUserResponse;
import ru.stepanov.selfcontrol.api.contract.scenario.ScenarioTemplateResponse;
import ru.stepanov.selfcontrol.api.mapper.AdminUserMapper;
import ru.stepanov.selfcontrol.api.mapper.ScenarioMapper;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.identity.User;
import ru.stepanov.selfcontrol.identity.UserRepository;
import ru.stepanov.selfcontrol.identity.UserStatus;
import ru.stepanov.selfcontrol.scenario.ScenarioTemplate;
import ru.stepanov.selfcontrol.scenario.ScenarioTemplateRepository;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository users;
    private final ScenarioTemplateRepository templates;
    private final AuditService audit;
    private final AuthenticationFacade auth;

    public AdminController(UserRepository users,
                           ScenarioTemplateRepository templates,
                           AuditService audit,
                           AuthenticationFacade auth) {
        this.users = users;
        this.templates = templates;
        this.audit = audit;
        this.auth = auth;
    }

    @GetMapping("/users")
    PagedResponse<AdminUserResponse> users(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Page<User> result = users.findAll(PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageUtils.from(result.map(AdminUserMapper::toResponse));
    }

    @PatchMapping("/users/{userId}/status")
    @ResponseStatus(HttpStatus.OK)
    void updateUserStatus(@PathVariable UUID userId, @RequestBody AdminUpdateUserStatusRequest request) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserStatus status = switch (request.status()) {
            case Active -> UserStatus.Active;
            case Blocked -> UserStatus.Blocked;
        };
        user.setStatus(status);
        User saved = users.save(user);
        audit.record(auth.userId(), userId, status == UserStatus.Blocked ? "USER_BLOCKED" : "USER_UNBLOCKED", "USER", userId,
                Map.of("status", saved.getStatus().name()));
    }

    @PostMapping("/scenarios/templates")
    @ResponseStatus(HttpStatus.CREATED)
    ScenarioTemplateResponse createTemplate(@RequestBody AdminCreateScenarioTemplateRequest request) {
        ScenarioTemplate template = new ScenarioTemplate();
        template.setScenarioTypeCode(request.scenarioTypeCode());
        template.setName(request.name());
        template.setDescription(request.description());
        template.setPublished(true);
        ScenarioTemplate saved = templates.save(template);
        audit.record(auth.userId(), null, "SCENARIO_TEMPLATE_CREATED", "SCENARIO_TEMPLATE", saved.getScenarioId(), Map.of(
                "scenarioTypeCode", saved.getScenarioTypeCode()
        ));
        return ScenarioMapper.toTemplateResponse(saved);
    }
}
