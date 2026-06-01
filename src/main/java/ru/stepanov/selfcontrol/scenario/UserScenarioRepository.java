package ru.stepanov.selfcontrol.scenario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserScenarioRepository extends JpaRepository<UserScenario, UUID> {
    List<UserScenario> findByUserId(UUID userId);

    List<UserScenario> findByUserIdAndActiveTrue(UUID userId);
}
