package ru.stepanov.selfcontrol.rabbit;

import java.util.UUID;

public record DebitConfigDto(String debitAmount, String currency, String recipientPaymentToken, UUID consentId,
                             UUID sourceAccountId) {
}
