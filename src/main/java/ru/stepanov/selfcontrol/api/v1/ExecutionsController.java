package ru.stepanov.selfcontrol.api.v1;

import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.api.contract.PagedResponse;
import ru.stepanov.selfcontrol.api.contract.execution.ExecutionResponse;
import ru.stepanov.selfcontrol.scenario.ExecutionQueryService;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.UUID;

@RestController
public class ExecutionsController {

    private final ExecutionQueryService executions;
    private final AuthenticationFacade auth;

    public ExecutionsController(ExecutionQueryService executions, AuthenticationFacade auth) {
        this.executions = executions;
        this.auth = auth;
    }

    @GetMapping("/api/v1/scenarios/{userScenarioId}/executions")
    PagedResponse<ExecutionResponse> list(@PathVariable UUID userScenarioId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return executions.listForScenario(auth.userId(), userScenarioId, page, size);
    }

    @GetMapping("/api/v1/executions/{executionId}")
    ExecutionResponse get(@PathVariable UUID executionId) {
        return executions.getById(auth.userId(), executionId);
    }
}
