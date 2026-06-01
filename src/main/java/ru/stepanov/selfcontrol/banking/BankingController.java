package ru.stepanov.selfcontrol.banking;

import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.*;

@RestController
@RequestMapping("/api/v1/banking")
public class BankingController {
    private final LinkedAccountRepository accounts;
    private final AcceptanceRepository acceptances;
    private final AuthenticationFacade auth;

    public BankingController(LinkedAccountRepository accounts, AcceptanceRepository acceptances, AuthenticationFacade auth) {
        this.accounts = accounts;
        this.acceptances = acceptances;
        this.auth = auth;
    }

    @GetMapping("/accounts")
    List<LinkedAccount> accounts() {
        return accounts.findByUserId(auth.userId());
    }

    @PostMapping("/accounts")
    LinkedAccount add(@RequestBody LinkedAccount a) {
        a.setUserId(auth.userId());
        return accounts.save(a);
    }

    @GetMapping("/acceptances")
    List<Acceptance> acceptances() {
        return acceptances.findByUserId(auth.userId());
    }

    @PostMapping("/acceptances")
    Acceptance createAcceptance(@RequestBody Acceptance a) {
        a.setUserId(auth.userId());
        return acceptances.save(a);
    }

    @PostMapping("/acceptances/{id}/revoke")
    Acceptance revoke(@PathVariable UUID id) {
        Acceptance a = acceptances.findById(id).orElseThrow();
        a.setStatus(AcceptanceStatus.Revoked);
        return acceptances.save(a);
    }
}
