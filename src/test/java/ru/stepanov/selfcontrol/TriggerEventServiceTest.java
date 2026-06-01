package ru.stepanov.selfcontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.stepanov.selfcontrol.rabbit.DebitConfigDto;
import ru.stepanov.selfcontrol.rabbit.TriggerEventMessage;
import ru.stepanov.selfcontrol.scenario.DebitOperation;
import ru.stepanov.selfcontrol.scenario.ExecutionStatus;
import ru.stepanov.selfcontrol.scenario.ScenarioExecution;
import ru.stepanov.selfcontrol.scenario.ScenarioExecutionRepository;
import ru.stepanov.selfcontrol.scenario.TriggerEventService;
import ru.stepanov.selfcontrol.scenario.UserScenario;
import ru.stepanov.selfcontrol.scenario.UserScenarioRepository;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriggerEventServiceTest {
    @Mock
    private ScenarioExecutionRepository executions;
    @Mock
    private UserScenarioRepository scenarios;
    @Mock
    private SimulacrumClient simulacrum;

    private TriggerEventService service;

    @BeforeEach
    void setUp() {
        service = new TriggerEventService(executions, scenarios, simulacrum);
    }

    @Test
    void successfulHandleInitiatesDebitWithTriggerMessageDataAndCompletesFromSimulacrumResponse() {
        TriggerEventMessage message = message();
        UserScenario userScenario = new UserScenario();
        userScenario.setUserScenarioId(message.externalUserScenarioId());
        userScenario.setUserId(message.externalUserId());
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(userScenario));
        when(simulacrum.initiateDebit(eq(message.externalUserId()), any()))
                .thenReturn(new SimulacrumClient.InitiateDebitResponse("SIM-TX-1", "COMPLETED", null, null, null));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        ArgumentCaptor<SimulacrumClient.InitiateDebitRequest> requestCaptor = ArgumentCaptor.forClass(SimulacrumClient.InitiateDebitRequest.class);
        verify(simulacrum).initiateDebit(eq(message.externalUserId()), requestCaptor.capture());
        SimulacrumClient.InitiateDebitRequest request = requestCaptor.getValue();
        assertEquals(message.externalUserId(), request.userId());
        assertEquals(message.externalUserScenarioId(), request.userScenarioId());
        assertEquals(message.triggerEventId(), request.triggerEventId());
        assertEquals(message.triggerTransactionId(), request.triggerTransactionId());
        assertEquals(message.debitConfig().sourceAccountId(), request.sourceAccountId());
        assertEquals(message.debitConfig().consentId(), request.consentId());
        assertEquals(message.debitConfig().recipientPaymentToken(), request.recipientPaymentToken());
        assertEquals(new BigDecimal(message.debitConfig().debitAmount()), request.amount().amount());
        assertEquals(message.debitConfig().currency(), request.amount().currency());

        assertEquals(ExecutionStatus.DebitCompleted, execution.getStatus());
        DebitOperation operation = onlyOperation(execution);
        assertEquals(ExecutionStatus.DebitCompleted, operation.getStatus());
        assertEquals("SIM-TX-1", operation.getExternalTransactionID());
        assertNull(operation.getFailure());
        assertNotNull(operation.getCompletedAt());
        assertNotNull(execution.getCompletedAt());
        assertEquals(message.occurredAt(), userScenario.getLastTriggeredAt());
        verify(executions).save(execution);
    }

    @Test
    void simulacrumRejectionSavesDebitAndExecutionAsFailedWithFailureDetails() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(new UserScenario()));
        when(simulacrum.initiateDebit(eq(message.externalUserId()), any()))
                .thenReturn(new SimulacrumClient.InitiateDebitResponse("SIM-TX-DECLINED", "DECLINED", "LIMIT_EXCEEDED", "Daily limit exceeded", null));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        assertEquals(ExecutionStatus.DebitFailed, execution.getStatus());
        assertNotNull(execution.getCompletedAt());
        DebitOperation operation = onlyOperation(execution);
        assertEquals(ExecutionStatus.DebitFailed, operation.getStatus());
        assertEquals("SIM-TX-DECLINED", operation.getExternalTransactionID());
        assertEquals("LIMIT_EXCEEDED", operation.getFailure().getCode());
        assertEquals("Daily limit exceeded", operation.getFailure().getMessage());
        assertNotNull(operation.getCompletedAt());
        verify(executions).save(execution);
    }

    @Test
    void simulacrumExceptionSavesFailedDebitWithoutLocalSuccessfulTransactionOrCompletionTimestamp() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(new UserScenario()));
        when(simulacrum.initiateDebit(eq(message.externalUserId()), any()))
                .thenThrow(new IllegalStateException("Simulacrum unavailable"));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        assertEquals(ExecutionStatus.DebitFailed, execution.getStatus());
        assertNull(execution.getCompletedAt());
        DebitOperation operation = onlyOperation(execution);
        assertEquals(ExecutionStatus.DebitFailed, operation.getStatus());
        assertNull(operation.getExternalTransactionID());
        assertEquals("IllegalStateException", operation.getFailure().getCode());
        assertEquals("Simulacrum unavailable", operation.getFailure().getMessage());
        assertNull(operation.getCompletedAt());
        verify(executions).save(execution);
    }

    @Test
    void existingTriggerEventIsIdempotentAndDoesNotInitiateDebitOrSaveNewExecution() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(true);

        ScenarioExecution execution = service.handle(message);

        assertNull(execution);
        verify(scenarios, never()).findById(any());
        verify(simulacrum, never()).initiateDebit(any(), any());
        verify(executions, never()).save(any());
    }

    private DebitOperation onlyOperation(ScenarioExecution execution) {
        assertEquals(1, execution.getDebitOperations().size());
        DebitOperation operation = execution.getDebitOperations().get(0);
        assertSame(execution, operation.getScenarioExecution());
        return operation;
    }

    private TriggerEventMessage message() {
        return new TriggerEventMessage(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-06-01T10:15:30Z"),
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                UUID.fromString("40000000-0000-0000-0000-000000000004"),
                "TRIGGER-TX-1",
                "5813",
                "Coffee Shop",
                "350.00",
                "RUB",
                "UNDESIRABLE_PURCHASE",
                new DebitConfigDto(
                        "100.50",
                        "RUB",
                        "recipient-token-1",
                        UUID.fromString("50000000-0000-0000-0000-000000000005"),
                        UUID.fromString("60000000-0000-0000-0000-000000000006")
                )
        );
    }
}
