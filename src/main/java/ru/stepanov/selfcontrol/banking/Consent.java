package ru.stepanov.selfcontrol.banking;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Предварительно данный акцепт (consent) на один {@link LinkedAccount}.
 * Таблица {@code acceptances} — историческое имя.
 */
@Entity
@Table(name = "acceptances")
public class Consent {

    @Id
    @Column(name = "acceptance_id")
    private UUID consentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "linked_account_id", nullable = false, unique = true)
    private UUID linkedAccountId;

    @Column(name = "external_consent_id")
    private String externalConsentId;

    @Enumerated(EnumType.STRING)
    private AcceptanceStatus status;

    private Instant grantedAt;
    private Instant expiresAt;
    private Instant revokedAt;

    @Embedded
    private AcceptanceLimit acceptanceLimit;

    @PrePersist
    void prePersist() {
        if (consentId == null) {
            consentId = UUID.randomUUID();
        }
        if (status == null) {
            status = AcceptanceStatus.Active;
        }
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
    }

    public UUID getConsentId() {
        return consentId;
    }

    public void setConsentId(UUID consentId) {
        this.consentId = consentId;
    }

    /** @deprecated use {@link #getConsentId()} */
    @Deprecated
    public UUID getAcceptanceId() {
        return consentId;
    }

    /** @deprecated use {@link #setConsentId(UUID)} */
    @Deprecated
    public void setAcceptanceId(UUID acceptanceId) {
        this.consentId = acceptanceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getLinkedAccountId() {
        return linkedAccountId;
    }

    public void setLinkedAccountId(UUID linkedAccountId) {
        this.linkedAccountId = linkedAccountId;
    }

    public String getExternalConsentId() {
        return externalConsentId;
    }

    public void setExternalConsentId(String externalConsentId) {
        this.externalConsentId = externalConsentId;
    }

    public AcceptanceStatus getStatus() {
        return status;
    }

    public void setStatus(AcceptanceStatus status) {
        this.status = status;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public AcceptanceLimit getAcceptanceLimit() {
        return acceptanceLimit;
    }

    public void setAcceptanceLimit(AcceptanceLimit acceptanceLimit) {
        this.acceptanceLimit = acceptanceLimit;
    }
}
