package ru.stepanov.selfcontrol.scenario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ScenarioTemplateRepository extends JpaRepository<ScenarioTemplate, UUID> {
    Optional<ScenarioTemplate> findFirstByScenarioTypeCode(String code);

    List<ScenarioTemplate> findByPublishedTrue();

    @Query("select distinct t from ScenarioTemplate t join t.mccCodes mcc where mcc in :mccCodes")
    List<ScenarioTemplate> findByAnyMccCode(@Param("mccCodes") Collection<String> mccCodes);

    @Query("select distinct t from ScenarioTemplate t join t.mccCodes mcc where mcc in :mccCodes and t.scenarioId <> :scenarioId")
    List<ScenarioTemplate> findByAnyMccCodeAndScenarioIdNot(@Param("mccCodes") Collection<String> mccCodes, @Param("scenarioId") UUID scenarioId);
}
