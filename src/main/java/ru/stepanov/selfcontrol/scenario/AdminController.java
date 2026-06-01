package ru.stepanov.selfcontrol.scenario;

import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.identity.*;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumClient;

import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final ScenarioTemplateRepository templates;
    private final UserRepository users;
    private final ScenarioService scenarios;
    private final ScenarioExecutionRepository executions;
    private final SimulacrumClient simulacrum;

    public AdminController(ScenarioTemplateRepository templates, UserRepository users, ScenarioService scenarios, ScenarioExecutionRepository executions, SimulacrumClient simulacrum) {
        this.templates = templates;
        this.users = users;
        this.scenarios = scenarios;
        this.executions = executions;
        this.simulacrum = simulacrum;
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
        return templates.save(t);
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

    @PutMapping("/scenario-templates/{id}")
    ScenarioTemplate updateTemplate(@PathVariable UUID id, @RequestBody ScenarioTemplate r) {
        ScenarioTemplate t = templates.findById(id).orElseThrow();
        t.setName(r.getName());
        t.setDescription(r.getDescription());
        t.setPublished(r.isPublished());
        return templates.save(t);
    }

    @DeleteMapping("/scenario-templates/{id}")
    void deleteTemplate(@PathVariable UUID id) {
        templates.deleteById(id);
    }

    @GetMapping("/users")
    List<User> users() {
        return users.findAll();
    }

    @PostMapping("/users/{id}/block")
    User block(@PathVariable UUID id) {
        User u = users.findById(id).orElseThrow();
        u.setStatus(UserStatus.Blocked);
        return users.save(u);
    }

    @PostMapping("/users/{id}/unblock")
    User unblock(@PathVariable UUID id) {
        User u = users.findById(id).orElseThrow();
        u.setStatus(UserStatus.Active);
        return users.save(u);
    }

    @PostMapping("/scenarios/{id}/deactivate")
    void adminDeactivate(@PathVariable UUID id) {
        scenarios.deactivate(null, id, true);
    }

    @GetMapping("/users/{id}/audit")
    List<Map<String, String>> audit(@PathVariable UUID id) {
        return List.of(Map.of("event", "AUDIT_STUB", "userId", id.toString()));
    }

    @GetMapping("/simulacrum-api-log")
    List<SimulacrumClient.ApiLog> simulacrumLog() {
        return simulacrum.log();
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
