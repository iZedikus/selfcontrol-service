package ru.stepanov.selfcontrol.scenario;

import java.util.List;

public record CreateScenarioTemplateRequest(
        String scenarioTypeCode,
        String name,
        String description,
        boolean published,
        List<String> mccCodes
) {
    public CreateScenarioTemplateRequest {
        if (mccCodes == null) mccCodes = List.of();
    }
}
