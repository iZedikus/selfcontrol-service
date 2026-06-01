package ru.stepanov.selfcontrol.scenario;

import java.util.List;

public record UpdateScenarioTemplateRequest(
        String scenarioTypeCode,
        String name,
        String description,
        boolean published,
        List<String> mccCodes
) {
    public UpdateScenarioTemplateRequest {
        if (mccCodes == null) mccCodes = List.of();
    }
}
