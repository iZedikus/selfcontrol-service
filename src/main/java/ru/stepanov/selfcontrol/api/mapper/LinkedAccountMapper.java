package ru.stepanov.selfcontrol.api.mapper;

import ru.stepanov.selfcontrol.api.contract.account.LinkedAccountResponse;
import ru.stepanov.selfcontrol.api.contract.account.LinkedAccountStatus;
import ru.stepanov.selfcontrol.banking.LinkedAccount;

public final class LinkedAccountMapper {

    private LinkedAccountMapper() {
    }

    public static LinkedAccountResponse toResponse(LinkedAccount account) {
        return new LinkedAccountResponse(
                account.getLinkedAccountId(),
                null,
                account.getMaskedPAN(),
                account.getBankBIC() == null ? null : account.getBankBIC().getValue(),
                account.getDisplayName(),
                account.getCurrency() == null ? null : account.getCurrency().name(),
                mapStatus(account.getStatus()),
                account.getLinkedAt()
        );
    }

    private static LinkedAccountStatus mapStatus(ru.stepanov.selfcontrol.banking.LinkedAccountStatus status) {
        if (status == null) {
            return LinkedAccountStatus.Active;
        }
        return switch (status) {
            case PendingVerification -> LinkedAccountStatus.PendingVerification;
            case Active -> LinkedAccountStatus.Active;
            case Revoked -> LinkedAccountStatus.Revoked;
            case Expired -> LinkedAccountStatus.Expired;
        };
    }
}
