package ru.stepanov.selfcontrol.banking;

import jakarta.persistence.*;
import ru.stepanov.selfcontrol.common.CurrencyCode;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "linked_accounts")
public class LinkedAccount {
    @Id
    @Column(name = "linked_account_id")
    private UUID linkedAccountId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_id")
    private Acceptance acceptance;
    @Column(name = "external_account_id")
    private String externalAccountId;
    private String displayName;
    @Column(name = "masked_pan")
    private String maskedPAN;
    @Embedded
    private PaymentToken paymentToken;
    @Embedded
    private BankBIC bankBIC;
    @Column(name = "bank_name")
    private String bankName;
    @Enumerated(EnumType.STRING)
    private CurrencyCode currency = CurrencyCode.RUB;
    private Instant linkedAt;
    private Instant expiresAt;
    @Enumerated(EnumType.STRING)
    private LinkedAccountStatus status;

    @PrePersist
    void pre() {
        if (linkedAccountId == null) linkedAccountId = UUID.randomUUID();
        if (linkedAt == null) linkedAt = Instant.now();
        if (status == null) status = LinkedAccountStatus.Active;
    }

    public UUID getLinkedAccountId() {
        return linkedAccountId;
    }

    public void setLinkedAccountId(UUID linkedAccountId) {
        this.linkedAccountId = linkedAccountId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Acceptance getAcceptance() {
        return acceptance;
    }

    public void setAcceptance(Acceptance acceptance) {
        this.acceptance = acceptance;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public void setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMaskedPAN() {
        return maskedPAN;
    }

    public void setMaskedPAN(String maskedPAN) {
        this.maskedPAN = maskedPAN;
    }

    public PaymentToken getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(PaymentToken paymentToken) {
        this.paymentToken = paymentToken;
    }

    public BankBIC getBankBIC() {
        return bankBIC;
    }

    public void setBankBIC(BankBIC bankBIC) {
        this.bankBIC = bankBIC;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyCode currency) {
        this.currency = currency;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LinkedAccountStatus getStatus() {
        return status;
    }

    public void setStatus(LinkedAccountStatus status) {
        this.status = status;
    }
}
