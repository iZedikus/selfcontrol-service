package ru.stepanov.selfcontrol.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;
import ru.stepanov.selfcontrol.api.contract.PagedResponse;
import ru.stepanov.selfcontrol.api.contract.PageMeta;
import ru.stepanov.selfcontrol.api.contract.execution.DebitOperationResponse;
import ru.stepanov.selfcontrol.api.contract.execution.DebitOperationStatus;
import ru.stepanov.selfcontrol.api.contract.execution.ExecutionResponse;
import ru.stepanov.selfcontrol.api.contract.execution.ExecutionStatus;
import ru.stepanov.selfcontrol.api.contract.execution.TriggerSnapshotResponse;
import ru.stepanov.selfcontrol.api.v1.support.ContractControllerTestSupport;
import ru.stepanov.selfcontrol.scenario.ExecutionQueryService;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExecutionsControllerTest {

    @Mock
    private ExecutionQueryService executions;
    @Mock
    private AuthenticationFacade auth;

    @InjectMocks
    private ExecutionsController controller;

    private MockMvc mockMvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = ContractControllerTestSupport.mockMvc(controller);
        userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void listExecutionsReturnsPagedContractFields() throws Exception {
        UUID userScenarioId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(auth.userId()).thenReturn(userId);
        ExecutionResponse execution = sampleExecution(userScenarioId);
        when(executions.listForScenario(userId, userScenarioId, 0, 20))
                .thenReturn(new PagedResponse<>(List.of(execution), new PageMeta(0, 20, 1, 1)));

        mockMvc.perform(get("/api/v1/scenarios/{userScenarioId}/executions", userScenarioId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].executionId").value(execution.executionId().toString()))
                .andExpect(jsonPath("$.content[0].status").value("Completed"))
                .andExpect(jsonPath("$.content[0].triggerSnapshot.merchantName").value("Coffee Shop"))
                .andExpect(jsonPath("$.content[0].debitOperation.externalTransactionId").value("SIM-TX-1"))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void getExecutionReturnsContractFields() throws Exception {
        UUID executionId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID userScenarioId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(auth.userId()).thenReturn(userId);
        ExecutionResponse execution = sampleExecution(userScenarioId);
        when(executions.getById(userId, executionId)).thenReturn(execution);

        mockMvc.perform(get("/api/v1/executions/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(execution.executionId().toString()))
                .andExpect(jsonPath("$.userScenarioId").value(userScenarioId.toString()))
                .andExpect(jsonPath("$.debitOperation.status").value("AcceptedSettlementCompleted"));
    }

    @Test
    void getExecutionNotFoundReturns404ErrorResponse() throws Exception {
        UUID executionId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(auth.userId()).thenReturn(userId);
        when(executions.getById(userId, executionId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found"));

        mockMvc.perform(get("/api/v1/executions/{executionId}", executionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value(ErrorCode.NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("Execution not found"));
    }

    private ExecutionResponse sampleExecution(UUID userScenarioId) {
        return new ExecutionResponse(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                userScenarioId,
                ExecutionStatus.Completed,
                new TriggerSnapshotResponse("TX-1", "5813", "Coffee Shop", "350.00", "RUB",
                        Instant.parse("2026-06-01T10:15:30.000Z")),
                new DebitOperationResponse(
                        UUID.fromString("55555555-5555-5555-5555-555555555555"),
                        "SIM-TX-1",
                        "200.00",
                        DebitOperationStatus.AcceptedSettlementCompleted,
                        null,
                        null,
                        Instant.parse("2026-06-01T10:15:31.000Z"),
                        Instant.parse("2026-06-01T10:15:35.000Z")
                ),
                Instant.parse("2026-06-01T10:15:30.000Z"),
                Instant.parse("2026-06-01T10:15:35.000Z")
        );
    }
}
