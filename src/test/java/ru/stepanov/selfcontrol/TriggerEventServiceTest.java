package ru.stepanov.selfcontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.stepanov.selfcontrol.banking.Consent;
import ru.stepanov.selfcontrol.banking.ConsentRepository;
import ru.stepanov.selfcontrol.banking.LinkedAccount;
import ru.stepanov.selfcontrol.banking.LinkedAccountRepository;
import ru.stepanov.selfcontrol.rabbit.DebitConfigDto;
import ru.stepanov.selfcontrol.rabbit.TriggerEventMessage;
import ru.stepanov.selfcontrol.scenario.DebitOperation;
import ru.stepanov.selfcontrol.scenario.DebitOperationStatus;
import ru.stepanov.selfcontrol.scenario.ExecutionStatus;
import ru.stepanov.selfcontrol.scenario.ScenarioExecution;
import ru.stepanov.selfcontrol.scenario.ScenarioExecutionRepository;
import ru.stepanov.selfcontrol.scenario.TriggerEventService;
import ru.stepanov.selfcontrol.scenario.UserScenario;
import ru.stepanov.selfcontrol.scenario.UserScenarioRepository;
import ru.stepanov.selfcontrol.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;
import ru.stepanov.selfcontrol.simulacrum.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriggerEventServiceTest {
    @Mock
    private ScenarioExecutionRepository executions;
    @Mock
    private UserScenarioRepository scenarios;
    @Mock
    private LinkedAccountRepository linkedAccounts;
    @Mock
    private ConsentRepository consents;
    @Mock
    private SimulacrumClient simulacrum;
    @Mock
    private DebitStatusPoller debitStatusPoller;
    @Mock
    private NotificationService notifications;

    private TriggerEventService service;

    @BeforeEach
    void setUp() {
        service = new TriggerEventService(executions, scenarios, linkedAccounts, consents, simulacrum, debitStatusPoller, notifications);
    }

    @Test
    void successfulHandleSubmitsDebitPollsStatusAndCompletes() {
        TriggerEventMessage message = message();
        UserScenario userScenario = new UserScenario();
        userScenario.setUserScenarioId(message.externalUserScenarioId());
        userScenario.setUserId(message.externalUserId());

        LinkedAccount source = linkedAccount(message.debitConfig().sourceAccountId(), "ACC-SRC");
        Consent consent = consent(message.debitConfig().sourceAccountId(), message.debitConfig().consentId(), "consent-ext-1");

        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(userScenario));
        when(linkedAccounts.findById(message.debitConfig().sourceAccountId())).thenReturn(Optional.of(source));
        when(consents.findByLinkedAccountId(message.debitConfig().sourceAccountId())).thenReturn(Optional.of(consent));
        when(simulacrum.submitDebit(eq(message.externalUserId()), any()))
                .thenReturn(new PaymentDebitSubmitResponse("SIM-TX-1", DebitStatuses.PENDING));
        when(debitStatusPoller.pollUntilFinal(message.externalUserId(), "SIM-TX-1"))
                .thenReturn(new PaymentStatusResponse("SIM-TX-1", DebitStatuses.COMPLETED, null, null));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        ArgumentCaptor<PaymentDebitRequest> requestCaptor = ArgumentCaptor.forClass(PaymentDebitRequest.class);
        verify(simulacrum).submitDebit(eq(message.externalUserId()), requestCaptor.capture());
        PaymentDebitRequest request = requestCaptor.getValue();
        assertEquals("consent-ext-1", request.consentId());
        assertEquals("ACC-SRC", request.sourceAccountId());
        assertEquals(message.debitConfig().recipientPaymentToken(), request.recipientPaymentToken());
        assertEquals("100.50", request.amount());
        assertEquals("RUB", request.currency());
        verify(debitStatusPoller).pollUntilFinal(message.externalUserId(), "SIM-TX-1");

        assertEquals(ExecutionStatus.Completed, execution.getStatus());
        DebitOperation operation = onlyOperation(execution);
        assertEquals(DebitOperationStatus.AcceptedSettlementCompleted, operation.getStatus());
        assertEquals("SIM-TX-1", operation.getExternalTransactionID());
        assertNull(operation.getFailure());
        assertNotNull(operation.getCompletedAt());
        verify(executions).save(execution);
        verify(notifications).notifyScenarioTriggered(execution);
        verify(notifications).notifyDebitOutcome(execution);
    }

    @Test
    void rejectedDebitStatusSavesFailureDetails() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(new UserScenario()));
        when(linkedAccounts.findById(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(linkedAccount(message.debitConfig().sourceAccountId(), "ACC-SRC")));
        when(consents.findByLinkedAccountId(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(consent(message.debitConfig().sourceAccountId(), message.debitConfig().consentId(), "consent-ext-1")));
        when(simulacrum.submitDebit(eq(message.externalUserId()), any()))
                .thenReturn(new PaymentDebitSubmitResponse("SIM-TX-DECLINED", DebitStatuses.PENDING));
        when(debitStatusPoller.pollUntilFinal(message.externalUserId(), "SIM-TX-DECLINED"))
                .thenReturn(new PaymentStatusResponse("SIM-TX-DECLINED", DebitStatuses.REJECTED, "LIMIT_EXCEEDED", "Daily limit exceeded"));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        assertEquals(ExecutionStatus.Failed, execution.getStatus());
        DebitOperation operation = onlyOperation(execution);
        assertEquals(DebitOperationStatus.Rejected, operation.getStatus());
        assertEquals("LIMIT_EXCEEDED", operation.getFailure().getCode());
        assertEquals("Daily limit exceeded", operation.getFailure().getMessage());
    }

    @Test
    void pollingTimeoutMarksExecutionFailed() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(new UserScenario()));
        when(linkedAccounts.findById(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(linkedAccount(message.debitConfig().sourceAccountId(), "ACC-SRC")));
        when(consents.findByLinkedAccountId(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(consent(message.debitConfig().sourceAccountId(), message.debitConfig().consentId(), "consent-ext-1")));
        when(simulacrum.submitDebit(eq(message.externalUserId()), any()))
                .thenReturn(new PaymentDebitSubmitResponse("SIM-TX-PENDING", DebitStatuses.PENDING));
        when(debitStatusPoller.pollUntilFinal(message.externalUserId(), "SIM-TX-PENDING"))
                .thenThrow(new DebitStatusPollingException("timeout",
                        new PaymentStatusResponse("SIM-TX-PENDING", DebitStatuses.PENDING, null, null)));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        assertEquals(ExecutionStatus.Failed, execution.getStatus());
        assertEquals("DEBIT_STATUS_POLLING_TIMEOUT", onlyOperation(execution).getFailure().getCode());
    }

    @Test
    void simulacrumExceptionSavesFailedDebitWithoutCompletionTimestamp() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(new UserScenario()));
        when(linkedAccounts.findById(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(linkedAccount(message.debitConfig().sourceAccountId(), "ACC-SRC")));
        when(consents.findByLinkedAccountId(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(consent(message.debitConfig().sourceAccountId(), message.debitConfig().consentId(), "consent-ext-1")));
        when(simulacrum.submitDebit(eq(message.externalUserId()), any()))
                .thenThrow(new IllegalStateException("Simulacrum unavailable"));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        assertEquals(ExecutionStatus.Failed, execution.getStatus());
        DebitOperation operation = onlyOperation(execution);
        assertNull(operation.getExternalTransactionID());
        assertEquals("IllegalStateException", operation.getFailure().getCode());
        verify(debitStatusPoller, never()).pollUntilFinal(any(), any());
    }

    @Test
    void simulacrumConflictOnSubmitMarksExecutionFailed() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(false);
        when(scenarios.findById(message.externalUserScenarioId())).thenReturn(Optional.of(new UserScenario()));
        when(linkedAccounts.findById(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(linkedAccount(message.debitConfig().sourceAccountId(), "ACC-SRC")));
        when(consents.findByLinkedAccountId(message.debitConfig().sourceAccountId()))
                .thenReturn(Optional.of(consent(message.debitConfig().sourceAccountId(), message.debitConfig().consentId(), "consent-ext-1")));
        when(simulacrum.submitDebit(eq(message.externalUserId()), any()))
                .thenThrow(new RestClientResponseException("409 Conflict", HttpStatus.CONFLICT.value(), "409 Conflict",
                        null, """
                                {"status":409,"error":"CONSENT_INACTIVE","message":"Consent is not active"}
                                """.getBytes(), null));
        when(executions.save(any(ScenarioExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioExecution execution = service.handle(message);

        assertEquals(ExecutionStatus.Failed, execution.getStatus());
        assertEquals("RestClientResponseException", onlyOperation(execution).getFailure().getCode());
        verify(debitStatusPoller, never()).pollUntilFinal(any(), any());
    }

    @Test
    void existingTriggerEventIsIdempotent() {
        TriggerEventMessage message = message();
        when(executions.existsByTriggerEventId(message.triggerEventId())).thenReturn(true);

        assertNull(service.handle(message));

        verify(simulacrum, never()).submitDebit(any(), any());
        verify(executions, never()).save(any());
    }

    private LinkedAccount linkedAccount(UUID id, String externalAccountId) {
        LinkedAccount account = new LinkedAccount();
        account.setLinkedAccountId(id);
        account.setExternalAccountId(externalAccountId);
        return account;
    }

    private Consent consent(UUID linkedAccountId, UUID consentId, String externalConsentId) {
        Consent consent = new Consent();
        consent.setConsentId(consentId);
        consent.setLinkedAccountId(linkedAccountId);
        consent.setExternalConsentId(externalConsentId);
        return consent;
    }

    private DebitOperation onlyOperation(ScenarioExecution execution) {
        assertEquals(1, execution.getDebitOperations().size());
        return execution.getDebitOperations().get(0);
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
