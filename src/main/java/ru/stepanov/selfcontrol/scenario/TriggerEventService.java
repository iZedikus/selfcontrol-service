package ru.stepanov.selfcontrol.scenario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.banking.Consent;
import ru.stepanov.selfcontrol.banking.ConsentRepository;
import ru.stepanov.selfcontrol.banking.LinkedAccount;
import ru.stepanov.selfcontrol.banking.LinkedAccountRepository;
import ru.stepanov.selfcontrol.common.*;
import ru.stepanov.selfcontrol.notification.NotificationService;
import ru.stepanov.selfcontrol.rabbit.TriggerEventMessage;
import ru.stepanov.selfcontrol.simulacrum.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TriggerEventService {
    private static final Logger log = LoggerFactory.getLogger(TriggerEventService.class);
    private final ScenarioExecutionRepository executions;
    private final UserScenarioRepository scenarios;
    private final LinkedAccountRepository linkedAccounts;
    private final ConsentRepository consents;
    private final SimulacrumClient simulacrum;
    private final DebitStatusPoller debitStatusPoller;
    private final NotificationService notifications;

    public TriggerEventService(ScenarioExecutionRepository executions,
                               UserScenarioRepository scenarios,
                               LinkedAccountRepository linkedAccounts,
                               ConsentRepository consents,
                               SimulacrumClient simulacrum,
                               DebitStatusPoller debitStatusPoller,
                               NotificationService notifications) {
        this.executions = executions;
        this.scenarios = scenarios;
        this.linkedAccounts = linkedAccounts;
        this.consents = consents;
        this.simulacrum = simulacrum;
        this.debitStatusPoller = debitStatusPoller;
        this.notifications = notifications;
    }

    @Transactional
    public ScenarioExecution handle(TriggerEventMessage m) {
        if (executions.existsByTriggerEventId(m.triggerEventId())) {
            return null;
        }
        UserScenario us = scenarios.findById(m.externalUserScenarioId()).orElse(null);
        if (us == null) {
            log.warn("UserScenario not found for externalUserScenarioId={}, skipping trigger event. " +
                    "Scenario may have been deactivated between Oracle publish and IS processing. triggerEventId={}",
                    m.externalUserScenarioId(), m.triggerEventId());
            return null;
        }
        ScenarioExecution e = new ScenarioExecution();
        e.setTriggerEventId(m.triggerEventId());
        e.setUserScenarioId(m.externalUserScenarioId());
        e.setUserId(m.externalUserId());
        e.setStatus(ExecutionStatus.DebitInitiated);
        e.setTriggeredAt(m.occurredAt());
        TriggerSnapshot s = new TriggerSnapshot();
        s.setTransactionID(m.triggerTransactionId());
        s.setMcc(m.matchedMcc());
        s.setMerchantName(m.matchedMerchantName());
        s.setAmount(new Money(new BigDecimal(m.matchedAmount()), CurrencyCode.valueOf(m.matchedCurrency())));
        s.setOccurredAt(m.occurredAt());
        s.setAttribute(m.scenarioTypeCode());
        e.setTriggerSnapshot(s);
        DebitOperation op = new DebitOperation();
        op.setScenarioExecution(e);
        op.setAmount(new Money(new BigDecimal(m.debitConfig().debitAmount()), CurrencyCode.valueOf(m.debitConfig().currency())));
        op.setStatus(DebitOperationStatus.Pending);
        e.getDebitOperations().add(op);
        us.setLastTriggeredAt(m.occurredAt());

        try {
            PaymentDebitRequest debitRequest = buildDebitRequest(m);
            PaymentDebitSubmitResponse submit = simulacrum.submitDebit(m.externalUserId(), debitRequest);
            if (submit.transactionId() == null || submit.transactionId().isBlank()) {
                markDebitFailed(e, op, new Failure("SIMULACRUM_DEBIT_MISSING_TRANSACTION_ID",
                        "Simulacrum debit response does not contain transaction id"));
            } else {
                op.setExternalTransactionID(submit.transactionId());
                if (submit.status() != null && !submit.status().isBlank()) {
                    op.setStatus(mapDebitOperationStatus(submit.status()));
                }
                PaymentStatusResponse status = debitStatusPoller.pollUntilFinal(m.externalUserId(), submit.transactionId());
                applyPaymentStatus(e, op, status);
            }
        } catch (DebitStatusPollingException ex) {
            if (ex.getLastStatus() != null) {
                op.setExternalTransactionID(ex.getLastStatus().transactionId());
                op.setStatus(mapDebitOperationStatus(ex.getLastStatus().status()));
            }
            markDebitFailed(e, op, new Failure("DEBIT_STATUS_POLLING_TIMEOUT", ex.getMessage()));
        } catch (Exception ex) {
            markDebitFailed(e, op, new Failure(ex.getClass().getSimpleName(), ex.getMessage()));
        }
        ScenarioExecution saved = executions.save(e);
        notifications.notifyScenarioTriggered(saved);
        notifications.notifyDebitOutcome(saved);
        return saved;
    }

    private PaymentDebitRequest buildDebitRequest(TriggerEventMessage m) {
        LinkedAccount source = linkedAccounts.findById(m.debitConfig().sourceAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Linked account not found: " + m.debitConfig().sourceAccountId()));

        Consent consent = consents.findByLinkedAccountId(source.getLinkedAccountId())
                .or(() -> consents.findById(m.debitConfig().consentId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Consent not found for debit on account: " + source.getLinkedAccountId()));

        String externalConsentId = consent.getExternalConsentId();
        if (externalConsentId == null || externalConsentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Consent has no external consent id");
        }
        return new PaymentDebitRequest(
                externalConsentId,
                resolveSimulacrumAccountId(source),
                m.debitConfig().recipientPaymentToken(),
                m.debitConfig().debitAmount(),
                m.debitConfig().currency()
        );
    }

    private String resolveSimulacrumAccountId(LinkedAccount account) {
        if (account.getExternalAccountId() != null && !account.getExternalAccountId().isBlank()) {
            return account.getExternalAccountId();
        }
        if (account.getPaymentToken() != null && account.getPaymentToken().getValue() != null
                && !account.getPaymentToken().getValue().isBlank()) {
            return account.getPaymentToken().getValue();
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Linked account has no Simulacrum accountId");
    }

    private void applyPaymentStatus(ScenarioExecution e, DebitOperation op, PaymentStatusResponse status) {
        Instant completedAt = Instant.now();
        op.setCompletedAt(completedAt);
        e.setCompletedAt(completedAt);
        op.setExternalTransactionID(status.transactionId());
        op.setStatus(mapDebitOperationStatus(status.status()));

        if (DebitStatuses.REJECTED.equals(status.status())) {
            markDebitFailed(e, op, new Failure(
                    status.failureCode() == null ? "SIMULACRUM_DEBIT_REJECTED" : status.failureCode(),
                    status.failureMessage()));
            return;
        }
        if (DebitStatuses.COMPLETED.equals(status.status())) {
            op.setFailure(null);
            e.setStatus(ExecutionStatus.Completed);
            return;
        }
        markDebitFailed(e, op, new Failure("SIMULACRUM_DEBIT_UNEXPECTED_STATUS",
                "Unexpected final debit status: " + status.status()));
    }

    private DebitOperationStatus mapDebitOperationStatus(String status) {
        if (status == null || status.isBlank()) {
            return DebitOperationStatus.Pending;
        }
        return switch (status) {
            case DebitStatuses.PENDING -> DebitOperationStatus.Pending;
            case DebitStatuses.IN_PROCESS -> DebitOperationStatus.AcceptedSettlementInProcess;
            case DebitStatuses.COMPLETED -> DebitOperationStatus.AcceptedSettlementCompleted;
            case DebitStatuses.REJECTED -> DebitOperationStatus.Rejected;
            default -> throw new IllegalStateException("Unsupported Simulacrum debit status: " + status);
        };
    }

    private void markDebitFailed(ScenarioExecution e, DebitOperation op, Failure failure) {
        op.setFailure(failure);
        e.setStatus(ExecutionStatus.Failed);
        if (op.getCompletedAt() == null) {
            op.setCompletedAt(Instant.now());
        }
        if (e.getCompletedAt() == null) {
            e.setCompletedAt(op.getCompletedAt());
        }
    }
}
