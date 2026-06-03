package ru.stepanov.selfcontrol.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.api.contract.account.LinkAccountRequest;
import ru.stepanov.selfcontrol.api.contract.account.LinkedAccountResponse;
import ru.stepanov.selfcontrol.api.mapper.LinkedAccountMapper;
import ru.stepanov.selfcontrol.banking.BankingAccountLifecycleService;
import ru.stepanov.selfcontrol.banking.LinkedAccountRepository;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.List;
import java.util.UUID;

/**
 * REST API привязанных счетов по REST_КОНТРАКТ.yaml ([IS / Accounts]).
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {

    private final LinkedAccountRepository accounts;
    private final BankingAccountLifecycleService accountLifecycle;
    private final AuthenticationFacade auth;

    public AccountsController(LinkedAccountRepository accounts,
                              BankingAccountLifecycleService accountLifecycle,
                              AuthenticationFacade auth) {
        this.accounts = accounts;
        this.accountLifecycle = accountLifecycle;
        this.auth = auth;
    }

    @GetMapping
    List<LinkedAccountResponse> list() {
        return accounts.findByUserId(auth.userId()).stream()
                .map(LinkedAccountMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LinkedAccountResponse link(@RequestBody LinkAccountRequest request) {
        var linked = accountLifecycle.linkAccount(auth.userId(), request);
        return LinkedAccountMapper.toResponse(linked);
    }

    @DeleteMapping("/{linkedAccountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unlink(@PathVariable UUID linkedAccountId) {
        accountLifecycle.unlinkAccount(auth.userId(), linkedAccountId);
    }
}
