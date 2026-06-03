package ru.stepanov.selfcontrol.scenario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.PagedResponse;
import ru.stepanov.selfcontrol.api.contract.PageUtils;
import ru.stepanov.selfcontrol.api.contract.execution.ExecutionResponse;
import ru.stepanov.selfcontrol.api.mapper.ExecutionMapper;

import java.util.UUID;

@Service
public class ExecutionQueryService {

    private final ScenarioExecutionRepository executions;
    private final UserScenarioRepository scenarios;

    public ExecutionQueryService(ScenarioExecutionRepository executions, UserScenarioRepository scenarios) {
        this.executions = executions;
        this.scenarios = scenarios;
    }

    public PagedResponse<ExecutionResponse> listForScenario(UUID userId, UUID userScenarioId, int page, int size) {
        UserScenario scenario = scenarios.findById(userScenarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario not found"));
        if (!userId.equals(scenario.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenario not found");
        }
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Page<ScenarioExecution> result = executions.findByUserScenarioIdOrderByTriggeredAtDesc(
                userScenarioId,
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "triggeredAt"))
        );
        return PageUtils.from(result.map(ExecutionMapper::toResponse));
    }

    public ExecutionResponse getById(UUID userId, UUID executionId) {
        ScenarioExecution execution = executions.findByExecutionIdAndUserId(executionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found"));
        return ExecutionMapper.toResponse(execution);
    }
}
