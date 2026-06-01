package ru.stepanov.selfcontrol.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, UUID> {
    List<LinkedAccount> findByUserId(UUID userId);

    Optional<LinkedAccount> findByUserIdAndExternalAccountId(UUID userId, String externalAccountId);
}
