package ru.stepanov.selfcontrol.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.api.contract.scenario.ActivateScenarioRequest;
import ru.stepanov.selfcontrol.api.contract.scenario.ScenarioTemplateResponse;
import ru.stepanov.selfcontrol.api.contract.scenario.UpdateScenarioRequest;
import ru.stepanov.selfcontrol.api.contract.scenario.UserScenarioResponse;
import ru.stepanov.selfcontrol.api.mapper.ScenarioMapper;
import ru.stepanov.selfcontrol.scenario.ScenarioService;
import ru.stepanov.selfcontrol.scenario.UserScenario;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchaseConfigRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scenarios")
public class ScenariosController {

    private final ScenarioService scenarioService;
    private final UndesirablePurchaseConfigRepository configs;
    private final AuthenticationFacade auth;

    public ScenariosController(ScenarioService scenarioService,
                               UndesirablePurchaseConfigRepository configs,
                               AuthenticationFacade auth) {
        this.scenarioService = scenarioService;
        this.configs = configs;
        this.auth = auth;
    }

    @GetMapping("/templates")
    List<ScenarioTemplateResponse> templates() {
        return scenarioService.catalog().stream()
                .map(ScenarioMapper::toTemplateResponse)
                .toList();
    }

    @GetMapping
    List<UserScenarioResponse> list() {
        return scenarioService.list(auth.userId()).stream()
                .map(scenario -> ScenarioMapper.toUserScenarioResponse(scenario, configs.findByUserScenarioId(scenario.getUserScenarioId()).orElse(null)))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserScenarioResponse activate(@RequestBody ActivateScenarioRequest request) {
        UserScenario scenario = scenarioService.activate(auth.userId(), request);
        return ScenarioMapper.toUserScenarioResponse(scenario, configs.findByUserScenarioId(scenario.getUserScenarioId()).orElse(null));
    }

    @PutMapping("/{userScenarioId}")
    UserScenarioResponse update(@PathVariable UUID userScenarioId, @RequestBody UpdateScenarioRequest request) {
        UserScenario scenario = scenarioService.update(auth.userId(), userScenarioId, request);
        return ScenarioMapper.toUserScenarioResponse(scenario, configs.findByUserScenarioId(scenario.getUserScenarioId()).orElse(null));
    }

    @DeleteMapping("/{userScenarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable UUID userScenarioId) {
        scenarioService.deactivate(auth.userId(), userScenarioId, true);
    }
}
