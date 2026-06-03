package ru.stepanov.selfcontrol.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.consent.ConsentResponse;
import ru.stepanov.selfcontrol.api.contract.consent.GrantConsentRequest;
import ru.stepanov.selfcontrol.api.mapper.ConsentMapper;
import ru.stepanov.selfcontrol.banking.*;
import ru.stepanov.selfcontrol.common.CurrencyCode;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * REST API предварительно данного акцепта по REST_КОНТРАКТ.yaml ([IS / Consents]).
 */
@RestController
@RequestMapping("/api/v1/accounts/{linkedAccountId}/consent")
public class ConsentsController {

    private final LinkedAccountRepository accounts;
    private final AcceptanceService acceptanceService;
    private final AuthenticationFacade auth;

    public ConsentsController(LinkedAccountRepository accounts,
                              AcceptanceService acceptanceService,
                              AuthenticationFacade auth) {
        this.accounts = accounts;
        this.acceptanceService = acceptanceService;
        this.auth = auth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ConsentResponse grant(@PathVariable UUID linkedAccountId, @RequestBody GrantConsentRequest request) {
        ensureAccountOwned(linkedAccountId);
        Consent consent = acceptanceService.grant(auth.userId(), linkedAccountId, toGrantRequest(request));
        return ConsentMapper.toResponse(consent);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable UUID linkedAccountId) {
        ensureAccountOwned(linkedAccountId);
        acceptanceService.revokeByLinkedAccountId(auth.userId(), linkedAccountId);
    }

    private void ensureAccountOwned(UUID linkedAccountId) {
        LinkedAccount account = accounts.findById(linkedAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked account not found: " + linkedAccountId));
        if (!auth.userId().equals(account.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Linked account belongs to another user");
        }
    }

    private GrantAcceptanceRequest toGrantRequest(GrantConsentRequest request) {
        AcceptanceLimit limit = new AcceptanceLimit();
        CurrencyCode currency = CurrencyCode.valueOf(request.currency());
        limit.setTotalDebitLimit(money(request.totalDebitLimit(), currency));
        if (request.maxSingleDebit() != null && !request.maxSingleDebit().isBlank()) {
            limit.setMaxSingleDebit(money(request.maxSingleDebit(), currency));
        }
        Instant expiresAt = request.expiresAt() != null
                ? request.expiresAt()
                : Instant.now().plus(365, ChronoUnit.DAYS);
        return GrantAcceptanceRequest.forAccount(limit, expiresAt, null, null, null);
    }

    private static Money money(String amount, CurrencyCode currency) {
        return new Money(new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP), currency);
    }
}
