package ru.stepanov.selfcontrol.banking;

import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.*;

@RestController
@RequestMapping("/api/v1/banking")
public class BankingController {
    private final LinkedAccountRepository accounts;
    private final AcceptanceService acceptanceService;
    private final BankingAccountLifecycleService accountLifecycle;
    private final AuthenticationFacade auth;

    public BankingController(LinkedAccountRepository accounts, AcceptanceService acceptanceService, BankingAccountLifecycleService accountLifecycle, AuthenticationFacade auth) {
        this.accounts = accounts;
        this.acceptanceService = acceptanceService;
        this.accountLifecycle = accountLifecycle;
        this.auth = auth;
    }

    @GetMapping("/accounts")
    List<LinkedAccount> accounts() {
        return accounts.findByUserId(auth.userId());
    }

    @PostMapping("/accounts")
    LinkedAccount add(@RequestBody LinkAccountRequest request) {
        return accountLifecycle.linkAccount(auth.userId(), request.accountId());
    }

    @DeleteMapping("/accounts/{id}")
    LinkedAccount unlink(@PathVariable UUID id) {
        return accountLifecycle.unlinkAccount(auth.userId(), id);
    }

    public record LinkAccountRequest(String accountId) {
    }

    @GetMapping("/acceptances")
    List<Acceptance> acceptances() {
        return acceptanceService.findUserAcceptances(auth.userId());
    }

    @PostMapping("/acceptances")
    Acceptance createAcceptance(@RequestBody GrantAcceptanceRequest request) {
        return acceptanceService.grant(auth.userId(), request);
    }

    @PostMapping("/acceptances/{id}/revoke")
    Acceptance revoke(@PathVariable UUID id) {
        return acceptanceService.revoke(auth.userId(), id);
    }
}
