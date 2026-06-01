package ru.stepanov.selfcontrol.scenario;

import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class ScenarioController {
    private final ScenarioService service;
    private final ScenarioExecutionRepository executions;
    private final AuthenticationFacade auth;
    private final AuditService audit;

    public ScenarioController(ScenarioService service, ScenarioExecutionRepository executions, AuthenticationFacade auth, AuditService audit) {
        this.service = service;
        this.executions = executions;
        this.auth = auth;
        this.audit = audit;
    }

    @GetMapping("/scenario-templates")
    List<ScenarioTemplate> catalog() {
        return service.catalog();
    }

    @GetMapping("/scenarios")
    List<UserScenario> scenarios() {
        return service.list(auth.userId());
    }

    @PostMapping("/scenarios")
    UserScenario activate(@RequestBody ScenarioService.ActivateScenarioRequest r) {
        return service.activate(auth.userId(), r);
    }

    @PutMapping("/scenarios/{id}")
    UserScenario update(@PathVariable UUID id, @RequestBody ScenarioService.UpdateScenarioRequest r) {
        return service.update(auth.userId(), id, r);
    }

    @PostMapping("/scenarios/{id}/deactivate")
    void deactivate(@PathVariable UUID id) {
        service.deactivate(auth.userId(), id, false);
    }

    @GetMapping("/scenario-executions")
    List<ScenarioExecution> history() {
        return executions.findByUserId(auth.userId());
    }

    @PostMapping("/debit-operations/{id}/dispute")
    Map<String, String> dispute(@PathVariable UUID id) {
        UUID userId = auth.userId();
        audit.record(userId, userId, "DEBIT_OPERATION_DISPUTED", "DEBIT_OPERATION", id, Map.of("status", "ACCEPTED_STUB"));
        return Map.of("status", "ACCEPTED_STUB", "debitOperationId", id.toString());
    }
}
