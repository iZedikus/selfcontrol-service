package ru.stepanov.selfcontrol.api.contract.scenario;

import java.util.UUID;

/**
 * Шаблон сценария из каталога по REST_КОНТРАКТ.yaml.
 */
public record ScenarioTemplateResponse(
        UUID templateId,
        String scenarioTypeCode,
        String name,
        String description,
        boolean isPublished
) {
}
