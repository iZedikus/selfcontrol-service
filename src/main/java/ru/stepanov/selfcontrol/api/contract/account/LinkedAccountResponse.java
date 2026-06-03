package ru.stepanov.selfcontrol.api.contract.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.stepanov.selfcontrol.api.contract.ContractDates;

import java.time.Instant;
import java.util.UUID;

/**
 * Привязанный счёт по REST_КОНТРАКТ.yaml.
 * <p>
 * В публичных ответах IS поле {@code paymentToken} не должно отдаваться клиенту (см. agent_instructions);
 * при маппинге из доменной модели — обнулять или не заполнять.
 */
public record LinkedAccountResponse(
        UUID linkedAccountId,
        String paymentToken,
        String maskedPan,
        String bankBic,
        String displayName,
        String currency,
        LinkedAccountStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ContractDates.INSTANT_PATTERN, timezone = ContractDates.INSTANT_TIMEZONE) Instant linkedAt
) {
}
