package ru.stepanov.selfcontrol.scenario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stepanov.selfcontrol.common.*;
import ru.stepanov.selfcontrol.rabbit.TriggerEventMessage;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumClient;

import java.math.*;
import java.time.*;

@Service
public class TriggerEventService {
    private final ScenarioExecutionRepository executions;
    private final UserScenarioRepository scenarios;
    private final SimulacrumClient simulacrum;

    public TriggerEventService(ScenarioExecutionRepository executions, UserScenarioRepository scenarios, SimulacrumClient simulacrum) {
        this.executions = executions;
        this.scenarios = scenarios;
        this.simulacrum = simulacrum;
    }

    @Transactional
    public ScenarioExecution handle(TriggerEventMessage m) {
        if (executions.existsByTriggerEventId(m.triggerEventId())) return null;
        UserScenario us = scenarios.findById(m.externalUserScenarioId()).orElseThrow();
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
        op.setStatus(ExecutionStatus.DebitInitiated);
        e.getDebitOperations().add(op);
        us.setLastTriggeredAt(m.occurredAt());

        try {
            SimulacrumClient.InitiateDebitResponse response = simulacrum.initiateDebit(m.externalUserId(), new SimulacrumClient.InitiateDebitRequest(
                    m.externalUserId(),
                    m.externalUserScenarioId(),
                    m.triggerEventId(),
                    m.triggerTransactionId(),
                    m.debitConfig().sourceAccountId(),
                    m.debitConfig().consentId(),
                    m.debitConfig().recipientPaymentToken(),
                    new SimulacrumClient.MoneyDto(new BigDecimal(m.debitConfig().debitAmount()), m.debitConfig().currency())
            ));
            op.setExternalTransactionID(response.transactionId() == null ? "IS-DEBIT-" + m.triggerEventId() : response.transactionId());
            op.setCompletedAt(Instant.now());
            e.setCompletedAt(Instant.now());
            if (isDebitFailure(response.status())) {
                op.setStatus(ExecutionStatus.DebitFailed);
                op.setFailure(new Failure(response.code() == null ? "SIMULACRUM_DEBIT_FAILED" : response.code(), response.message()));
                e.setStatus(ExecutionStatus.DebitFailed);
            } else {
                op.setStatus(ExecutionStatus.DebitCompleted);
                e.setStatus(ExecutionStatus.DebitCompleted);
            }
        } catch (Exception ex) {
            op.setExternalTransactionID("IS-DEBIT-" + m.triggerEventId());
            op.setStatus(ExecutionStatus.DebitFailed);
            op.setCompletedAt(Instant.now());
            op.setFailure(new Failure(ex.getClass().getSimpleName(), ex.getMessage()));
            e.setStatus(ExecutionStatus.DebitFailed);
            e.setCompletedAt(Instant.now());
        }
        return executions.save(e);
    }

    private boolean isDebitFailure(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return switch (status.toUpperCase()) {
            case "FAILED", "FAILURE", "DECLINED", "REJECTED", "ERROR", "CANCELLED", "CANCELED" -> true;
            default -> false;
        };
    }
}
