package ru.stepanov.selfcontrol.undesirable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UndesirablePurchaseConfigRepository extends JpaRepository<UndesirablePurchaseConfig, UUID> {
    Optional<UndesirablePurchaseConfig> findByUserScenarioId(UUID userScenarioId);
}
