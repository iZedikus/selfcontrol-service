package ru.stepanov.selfcontrol.api.mapper;

import ru.stepanov.selfcontrol.api.contract.consent.ConsentResponse;
import ru.stepanov.selfcontrol.api.contract.consent.ConsentStatus;
import ru.stepanov.selfcontrol.banking.Acceptance;
import ru.stepanov.selfcontrol.banking.LinkedAccount;
import ru.stepanov.selfcontrol.common.Money;

import java.math.RoundingMode;
import java.util.UUID;

public final class ConsentMapper {

    private ConsentMapper() {
    }

    public static ConsentResponse toResponse(Acceptance acceptance, UUID linkedAccountId) {
        return new ConsentResponse(
                acceptance.getAcceptanceId(),
                linkedAccountId,
                parseUuid(acceptance.getExternalConsentId()),
                mapStatus(acceptance.getStatus()),
                moneyString(acceptance.getAcceptanceLimit() == null ? null : acceptance.getAcceptanceLimit().getTotalDebitLimit()),
                moneyString(acceptance.getAcceptanceLimit() == null ? null : acceptance.getAcceptanceLimit().getMaxSingleDebit()),
                currencyCode(acceptance.getAcceptanceLimit()),
                acceptance.getGrantedAt(),
                acceptance.getExpiresAt()
        );
    }

    public static UUID linkedAccountIdFor(Acceptance acceptance) {
        return acceptance.getLinkedAccounts().stream()
                .findFirst()
                .map(LinkedAccount::getLinkedAccountId)
                .orElse(null);
    }

    private static ConsentStatus mapStatus(ru.stepanov.selfcontrol.banking.AcceptanceStatus status) {
        if (status == null) {
            return ConsentStatus.Active;
        }
        return switch (status) {
            case Pending -> ConsentStatus.Pending;
            case Active -> ConsentStatus.Active;
            case Revoked -> ConsentStatus.Revoked;
            case Expired -> ConsentStatus.Expired;
        };
    }

    private static String moneyString(Money money) {
        if (money == null || money.getAmount() == null) {
            return null;
        }
        return money.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String currencyCode(ru.stepanov.selfcontrol.banking.AcceptanceLimit limit) {
        if (limit == null || limit.getTotalDebitLimit() == null || limit.getTotalDebitLimit().getCurrency() == null) {
            return null;
        }
        return limit.getTotalDebitLimit().getCurrency().name();
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
