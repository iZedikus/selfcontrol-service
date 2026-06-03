package ru.stepanov.selfcontrol.scenario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScenarioExecutionRepository extends JpaRepository<ScenarioExecution, UUID> {
    boolean existsByTriggerEventId(UUID triggerEventId);

    List<ScenarioExecution> findByUserId(UUID userId);

    Page<ScenarioExecution> findByUserScenarioIdOrderByTriggeredAtDesc(UUID userScenarioId, Pageable pageable);

    Optional<ScenarioExecution> findByExecutionIdAndUserId(UUID executionId, UUID userId);
}
