package ru.stepanov.selfcontrol.api.contract.admin;

/**
 * POST /api/v1/admin/scenarios/templates
 */
public record AdminCreateScenarioTemplateRequest(
        String scenarioTypeCode,
        String name,
        String description
) {
}
