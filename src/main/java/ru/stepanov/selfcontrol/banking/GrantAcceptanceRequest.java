package ru.stepanov.selfcontrol.banking;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GrantAcceptanceRequest(
        List<UUID> linkedAccountIds,
        AcceptanceLimit acceptanceLimit,
        Instant expiresAt,
        String purpose,
        List<String> permissions,
        Map<String, Object> simulacrumParams
) {
    public static GrantAcceptanceRequest forAccount(AcceptanceLimit acceptanceLimit,
                                                     Instant expiresAt,
                                                     String purpose,
                                                     List<String> permissions,
                                                     Map<String, Object> simulacrumParams) {
        return new GrantAcceptanceRequest(List.of(), acceptanceLimit, expiresAt, purpose, permissions, simulacrumParams);
    }

    public GrantAcceptanceRequest withoutLinkedAccountIds() {
        return new GrantAcceptanceRequest(List.of(), acceptanceLimit, expiresAt, purpose, permissions, simulacrumParams);
    }
}
