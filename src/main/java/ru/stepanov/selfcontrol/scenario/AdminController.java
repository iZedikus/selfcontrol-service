package ru.stepanov.selfcontrol.scenario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.audit.AuditEvent;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.identity.*;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumApiLogEntry;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumApiLogRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final ScenarioTemplateRepository templates;
    private final UserScenarioRepository userScenarios;
    private final UserRepository users;
    private final ScenarioService scenarios;
    private final ScenarioExecutionRepository executions;
    private final SimulacrumApiLogRepository simulacrumApiLogRepository;
    private final AuditService audit;
    private final AuthenticationFacade auth;

    public AdminController(ScenarioTemplateRepository templates, UserScenarioRepository userScenarios, UserRepository users, ScenarioService scenarios, ScenarioExecutionRepository executions, SimulacrumApiLogRepository simulacrumApiLogRepository, AuditService audit, AuthenticationFacade auth) {
        this.templates = templates;
        this.userScenarios = userScenarios;
        this.users = users;
        this.scenarios = scenarios;
        this.executions = executions;
        this.simulacrumApiLogRepository = simulacrumApiLogRepository;
        this.audit = audit;
        this.auth = auth;
    }

    @PostMapping("/scenario-templates")
    ScenarioTemplate createTemplate(@RequestBody CreateScenarioTemplateRequest request) {
        Set<String> mccCodes = normalizeMccCodes(request.mccCodes());
        if (!mccCodes.isEmpty()) {
            Optional<ScenarioTemplate> existing = templates.findByAnyMccCode(mccCodes).stream().findFirst();
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        ScenarioTemplate t = new ScenarioTemplate();
        t.setScenarioTypeCode(request.scenarioTypeCode());
        t.setName(request.name());
        t.setDescription(request.description());
        t.setPublished(request.published());
        t.setMccCodes(mccCodes);
        ScenarioTemplate saved = templates.save(t);
        audit.record(auth.userId(), null, "SCENARIO_TEMPLATE_CREATED", "SCENARIO_TEMPLATE", saved.getScenarioId(), Map.of(
                "scenarioTypeCode", saved.getScenarioTypeCode(),
                "published", saved.isPublished()
        ));
        return saved;
    }

    private Set<String> normalizeMccCodes(Collection<String> mccCodes) {
        if (mccCodes == null) {
            return new LinkedHashSet<>();
        }
        return mccCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    @PutMapping("/scenario-templates/{id}")
    ScenarioTemplate updateTemplate(@PathVariable UUID id, @RequestBody UpdateScenarioTemplateRequest request) {
        ScenarioTemplate t = templates.findById(id).orElseThrow();
        Set<String> mccCodes = normalizeMccCodes(request.mccCodes());
        if (!mccCodes.equals(t.getMccCodes())) {
            assertMccCodesDoNotConflict(id, mccCodes);
        }
        t.setScenarioTypeCode(request.scenarioTypeCode());
        t.setName(request.name());
        t.setDescription(request.description());
        t.setPublished(request.published());
        t.getMccCodes().clear();
        t.getMccCodes().addAll(mccCodes);
        ScenarioTemplate saved = templates.save(t);
        audit.record(auth.userId(), null, "SCENARIO_TEMPLATE_UPDATED", "SCENARIO_TEMPLATE", saved.getScenarioId(), Map.of(
                "scenarioTypeCode", saved.getScenarioTypeCode(),
                "published", saved.isPublished()
        ));
        return saved;
    }

    private void assertMccCodesDoNotConflict(UUID scenarioId, Set<String> mccCodes) {
        if (mccCodes.isEmpty()) {
            return;
        }
        templates.findByAnyMccCodeAndScenarioIdNot(mccCodes, scenarioId).stream().findFirst().ifPresent(conflict -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MCC codes are already assigned to scenario template " + conflict.getScenarioId());
        });
    }

    @Transactional
    @PostMapping("/scenario-templates/{id}/deactivate")
    ScenarioTemplate deactivateTemplate(@PathVariable UUID id) {
        ScenarioTemplate t = templates.findById(id).orElseThrow();
        t.setPublished(false);
        ScenarioTemplate saved = templates.save(t);
        audit.record(auth.userId(), null, "SCENARIO_TEMPLATE_DEACTIVATED", "SCENARIO_TEMPLATE", saved.getScenarioId(), Map.of("published", saved.isPublished()));
        return saved;
    }

    @Transactional
    @DeleteMapping("/scenario-templates/{id}")
    void deleteTemplate(@PathVariable UUID id) {
        ScenarioTemplate t = templates.findById(id).orElseThrow();
        if (userScenarios.existsByTemplateScenarioIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Scenario template has active user scenarios and cannot be physically deleted");
        }
        if (userScenarios.existsByTemplateScenarioId(id)) {
            t.setPublished(false);
            templates.save(t);
            audit.record(auth.userId(), null, "SCENARIO_TEMPLATE_DEACTIVATED", "SCENARIO_TEMPLATE", t.getScenarioId(), Map.of("reason", "HAS_USER_SCENARIOS"));
            return;
        }
        templates.delete(t);
        audit.record(auth.userId(), null, "SCENARIO_TEMPLATE_DELETED", "SCENARIO_TEMPLATE", id, Map.of("physicalDelete", true));
    }

    @GetMapping("/users")
    List<User> users() {
        return users.findAll();
    }

    @PostMapping("/users/{id}/block")
    User block(@PathVariable UUID id) {
        User u = users.findById(id).orElseThrow();
        u.setStatus(UserStatus.Blocked);
        User saved = users.save(u);
        audit.record(auth.userId(), id, "USER_BLOCKED", "USER", id, Map.of("status", saved.getStatus().name()));
        return saved;
    }

    @PostMapping("/users/{id}/unblock")
    User unblock(@PathVariable UUID id) {
        User u = users.findById(id).orElseThrow();
        u.setStatus(UserStatus.Active);
        User saved = users.save(u);
        audit.record(auth.userId(), id, "USER_UNBLOCKED", "USER", id, Map.of("status", saved.getStatus().name()));
        return saved;
    }

    @PostMapping("/scenarios/{id}/deactivate")
    void adminDeactivate(@PathVariable UUID id) {
        scenarios.deactivate(null, id, true, auth.userId());
    }

    @GetMapping("/users/{id}/audit")
    Page<AuditEvent> audit(@PathVariable UUID id,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "50") int size,
                           @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return audit.findByUser(id, PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(direction, "createdAt")));
    }

    @GetMapping("/simulacrum-api-log")
    Page<SimulacrumApiLogEntry> simulacrumLog(@RequestParam(required = false) UUID userId,
                                               @RequestParam(required = false) String operationType,
                                               @RequestParam(required = false) Instant createdFrom,
                                               @RequestParam(required = false) Instant createdTo,
                                               @RequestParam(required = false, name = "status") Integer responseStatus,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size,
                                               @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return simulacrumApiLogRepository.findAll(
                simulacrumLogSpecification(userId, operationType, createdFrom, createdTo, responseStatus),
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(direction, "createdAt"))
        );
    }

    private Specification<SimulacrumApiLogEntry> simulacrumLogSpecification(UUID userId, String operationType,
                                                                            Instant createdFrom, Instant createdTo,
                                                                            Integer responseStatus) {
        Specification<SimulacrumApiLogEntry> spec = (root, query, cb) -> cb.conjunction();
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (operationType != null && !operationType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("operationType"), operationType));
        }
        if (createdFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        if (responseStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("responseStatus"), responseStatus));
        }
        return spec;
    }

    @GetMapping("/oracle-trigger-log")
    List<ScenarioExecution> oracleLog() {
        return executions.findAll();
    }

    @GetMapping("/statistics")
    Map<UUID, Long> statistics() {
        return executions.findAll().stream().collect(Collectors.groupingBy(ScenarioExecution::getUserScenarioId, Collectors.counting()));
    }
}
