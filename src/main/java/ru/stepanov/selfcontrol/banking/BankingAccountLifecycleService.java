package ru.stepanov.selfcontrol.banking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BankingAccountLifecycleService {
    private final LinkedAccountRepository accounts;
    private final AcceptanceRepository acceptances;

    public BankingAccountLifecycleService(LinkedAccountRepository accounts, AcceptanceRepository acceptances) {
        this.accounts = accounts;
        this.acceptances = acceptances;
    }

    @Transactional
    public void unlinkUserBanking(UUID userId) {
        Instant now = Instant.now();
        accounts.findByUserId(userId).forEach(account -> {
            account.setStatus(LinkedAccountStatus.Revoked);
            account.setPaymentToken(null);
            account.setAcceptance(null);
            account.setExpiresAt(now);
            accounts.save(account);
        });
        acceptances.findByUserId(userId).forEach(acceptance -> {
            acceptance.setStatus(AcceptanceStatus.Revoked);
            acceptance.setRevokedAt(now);
            acceptances.save(acceptance);
        });
    }
}
