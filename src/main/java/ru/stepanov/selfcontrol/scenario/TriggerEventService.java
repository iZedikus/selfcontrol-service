package ru.stepanov.selfcontrol.scenario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stepanov.selfcontrol.common.*;
import ru.stepanov.selfcontrol.rabbit.TriggerEventMessage;

import java.math.*;
import java.time.*;

@Service
public class TriggerEventService {
    private final ScenarioExecutionRepository executions;
    private final UserScenarioRepository scenarios;

    public TriggerEventService(ScenarioExecutionRepository executions, UserScenarioRepository scenarios) {
        this.executions = executions;
        this.scenarios = scenarios;
    }

    @Transactional
    public ScenarioExecution handle(TriggerEventMessage m) {
        if (executions.existsByTriggerEventId(m.triggerEventId())) return null;
        UserScenario us = scenarios.findById(m.externalUserScenarioId()).orElseThrow();
        ScenarioExecution e = new ScenarioExecution();
        e.setTriggerEventId(m.triggerEventId());
        e.setUserScenarioId(m.externalUserScenarioId());
        e.setUserId(m.externalUserId());
        e.setStatus(ExecutionStatus.DebitCompleted);
        e.setTriggeredAt(m.occurredAt());
        e.setCompletedAt(Instant.now());
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
        op.setExternalTransactionID("IS-DEBIT-" + m.triggerEventId());
        op.setAmount(new Money(new BigDecimal(m.debitConfig().debitAmount()), CurrencyCode.valueOf(m.debitConfig().currency())));
        op.setStatus(ExecutionStatus.DebitCompleted);
        op.setCompletedAt(Instant.now());
        e.getDebitOperations().add(op);
        us.setLastTriggeredAt(m.occurredAt());
        return executions.save(e);
    }
}
