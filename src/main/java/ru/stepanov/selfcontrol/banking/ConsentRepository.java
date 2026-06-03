package ru.stepanov.selfcontrol.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    List<Consent> findByUserId(UUID userId);

    Optional<Consent> findByLinkedAccountId(UUID linkedAccountId);

    boolean existsByLinkedAccountIdAndStatus(UUID linkedAccountId, AcceptanceStatus status);
}
