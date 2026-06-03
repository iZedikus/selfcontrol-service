package ru.stepanov.selfcontrol.api.mapper;

import ru.stepanov.selfcontrol.api.contract.consent.ConsentResponse;
import ru.stepanov.selfcontrol.api.contract.consent.ConsentStatus;
import ru.stepanov.selfcontrol.banking.Consent;
import ru.stepanov.selfcontrol.common.Money;

import java.math.RoundingMode;
import java.util.UUID;

public final class ConsentMapper {

    private ConsentMapper() {
    }

    public static ConsentResponse toResponse(Consent consent) {
        return new ConsentResponse(
                consent.getConsentId(),
                consent.getLinkedAccountId(),
                parseUuid(consent.getExternalConsentId()),
                mapStatus(consent.getStatus()),
                moneyString(consent.getAcceptanceLimit() == null ? null : consent.getAcceptanceLimit().getTotalDebitLimit()),
                moneyString(consent.getAcceptanceLimit() == null ? null : consent.getAcceptanceLimit().getMaxSingleDebit()),
                currencyCode(consent.getAcceptanceLimit()),
                consent.getGrantedAt(),
                consent.getExpiresAt()
        );
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
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
