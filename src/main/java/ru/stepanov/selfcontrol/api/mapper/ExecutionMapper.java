package ru.stepanov.selfcontrol.api.mapper;

import ru.stepanov.selfcontrol.api.contract.execution.DebitOperationResponse;
import ru.stepanov.selfcontrol.api.contract.execution.DebitOperationStatus;
import ru.stepanov.selfcontrol.api.contract.execution.ExecutionResponse;
import ru.stepanov.selfcontrol.api.contract.execution.ExecutionStatus;
import ru.stepanov.selfcontrol.api.contract.execution.TriggerSnapshotResponse;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.scenario.DebitOperation;
import ru.stepanov.selfcontrol.scenario.ScenarioExecution;
import ru.stepanov.selfcontrol.scenario.TriggerSnapshot;

import java.math.RoundingMode;

public final class ExecutionMapper {

    private ExecutionMapper() {
    }

    public static ExecutionResponse toResponse(ScenarioExecution execution) {
        DebitOperation debit = execution.getDebitOperations().isEmpty() ? null : execution.getDebitOperations().getFirst();
        return new ExecutionResponse(
                execution.getExecutionId(),
                execution.getUserScenarioId(),
                mapExecutionStatus(execution.getStatus()),
                mapTriggerSnapshot(execution.getTriggerSnapshot()),
                debit == null ? null : mapDebitOperation(debit),
                execution.getTriggeredAt(),
                execution.getCompletedAt()
        );
    }

    private static ExecutionStatus mapExecutionStatus(ru.stepanov.selfcontrol.scenario.ExecutionStatus status) {
        if (status == null) {
            return null;
        }
        return ExecutionStatus.valueOf(status.name());
    }

    private static DebitOperationStatus mapDebitStatus(ru.stepanov.selfcontrol.scenario.DebitOperationStatus status) {
        if (status == null) {
            return null;
        }
        return DebitOperationStatus.valueOf(status.name());
    }

    private static TriggerSnapshotResponse mapTriggerSnapshot(TriggerSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        Money amount = snapshot.getAmount();
        return new TriggerSnapshotResponse(
                snapshot.getTransactionID(),
                snapshot.getMcc(),
                snapshot.getMerchantName(),
                moneyString(amount),
                amount == null || amount.getCurrency() == null ? null : amount.getCurrency().name(),
                snapshot.getOccurredAt()
        );
    }

    private static DebitOperationResponse mapDebitOperation(DebitOperation operation) {
        return new DebitOperationResponse(
                operation.getDebitOperationId(),
                operation.getExternalTransactionID(),
                moneyString(operation.getAmount()),
                mapDebitStatus(operation.getStatus()),
                operation.getFailure() == null ? null : operation.getFailure().getCode(),
                operation.getFailure() == null ? null : operation.getFailure().getMessage(),
                operation.getInitiatedAt(),
                operation.getCompletedAt()
        );
    }

    private static String moneyString(Money money) {
        if (money == null || money.getAmount() == null) {
            return null;
        }
        return money.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
