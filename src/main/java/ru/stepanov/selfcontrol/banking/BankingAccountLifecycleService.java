package ru.stepanov.selfcontrol.banking;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.common.CurrencyCode;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class BankingAccountLifecycleService {
    private final LinkedAccountRepository accounts;
    private final AcceptanceRepository acceptances;
    private final SimulacrumClient simulacrum;
    private final AuditService audit;

    public BankingAccountLifecycleService(LinkedAccountRepository accounts, AcceptanceRepository acceptances, SimulacrumClient simulacrum, AuditService audit) {
        this.accounts = accounts;
        this.acceptances = acceptances;
        this.simulacrum = simulacrum;
        this.audit = audit;
    }

    @Transactional
    public LinkedAccount linkAccount(UUID userId, String externalAccountId) {
        if (externalAccountId == null || externalAccountId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId is required");
        }

        SimulacrumClient.Account externalAccount = simulacrum.getAccounts(userId).stream()
                .filter(account -> externalAccountId.equals(account.accountId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not available in Simulacrum"));

        LinkedAccount account = accounts.findByUserIdAndExternalAccountId(userId, externalAccount.accountId())
                .orElseGet(LinkedAccount::new);
        account.setUserId(userId);
        account.setExternalAccountId(externalAccount.accountId());
        account.setDisplayName(externalAccount.displayName());
        account.setMaskedPAN(externalAccount.maskedPan());
        account.setCurrency(toCurrency(externalAccount.currency()));
        account.setBankBIC(toBankBIC(externalAccount));
        account.setBankName(blankToNull(externalAccount.bank()));
        account.setPaymentToken(toPaymentToken(externalAccount));
        account.setStatus(toStatus(externalAccount.status()));
        account.setExpiresAt(null);
        LinkedAccount saved = accounts.save(account);
        audit.record(userId, userId, "BANK_ACCOUNT_LINKED", "LINKED_ACCOUNT", saved.getLinkedAccountId(), Map.of(
                "externalAccountId", saved.getExternalAccountId(),
                "status", saved.getStatus().name()
        ));
        return saved;
    }

    @Transactional
    public LinkedAccount unlinkAccount(UUID userId, UUID linkedAccountId) {
        LinkedAccount account = accounts.findById(linkedAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked account not found"));
        if (!userId.equals(account.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Linked account belongs to another user");
        }
        account.setStatus(LinkedAccountStatus.Revoked);
        account.setPaymentToken(null);
        account.setAcceptance(null);
        account.setExpiresAt(Instant.now());
        LinkedAccount saved = accounts.save(account);
        audit.record(userId, userId, "BANK_ACCOUNT_UNLINKED", "LINKED_ACCOUNT", saved.getLinkedAccountId(), Map.of(
                "externalAccountId", saved.getExternalAccountId(),
                "status", saved.getStatus().name()
        ));
        return saved;
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

    private CurrencyCode toCurrency(String value) {
        if (value == null || value.isBlank()) {
            return CurrencyCode.RUB;
        }
        try {
            return CurrencyCode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unsupported Simulacrum account currency: " + value, e);
        }
    }

    private BankBIC toBankBIC(SimulacrumClient.Account account) {
        String bankBic = blankToNull(account.bankBic());
        return bankBic == null ? null : new BankBIC(bankBic);
    }

    private PaymentToken toPaymentToken(SimulacrumClient.Account account) {
        return account.paymentToken() == null || account.paymentToken().isBlank() ? null : new PaymentToken(account.paymentToken());
    }

    private LinkedAccountStatus toStatus(String value) {
        if (value == null || value.isBlank()) {
            return LinkedAccountStatus.Active;
        }
        for (LinkedAccountStatus status : LinkedAccountStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return switch (value.toUpperCase()) {
            case "ACTIVE", "OPEN", "ENABLED" -> LinkedAccountStatus.Active;
            case "REVOKED", "UNLINKED", "CLOSED", "DISABLED", "INACTIVE" -> LinkedAccountStatus.Revoked;
            case "EXPIRED" -> LinkedAccountStatus.Expired;
            case "PENDING", "PENDING_VERIFICATION" -> LinkedAccountStatus.PendingVerification;
            default -> throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unsupported Simulacrum account status: " + value);
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
