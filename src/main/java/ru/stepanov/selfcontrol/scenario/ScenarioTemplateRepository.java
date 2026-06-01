package ru.stepanov.selfcontrol.scenario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ScenarioTemplateRepository extends JpaRepository<ScenarioTemplate, UUID> {
    Optional<ScenarioTemplate> findByScenarioTypeCode(String code);

    List<ScenarioTemplate> findByPublishedTrue();
}
