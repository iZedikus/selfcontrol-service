package ru.stepanov.selfcontrol.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.stepanov.selfcontrol.api.v1.support.ContractControllerTestSupport;
import ru.stepanov.selfcontrol.banking.PaymentToken;
import ru.stepanov.selfcontrol.common.CurrencyCode;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.scenario.DebitConfig;
import ru.stepanov.selfcontrol.scenario.ScenarioService;
import ru.stepanov.selfcontrol.scenario.ScenarioTemplate;
import ru.stepanov.selfcontrol.scenario.UserScenario;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchaseConfigRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScenariosControllerTest {

    @Mock
    private ScenarioService scenarioService;
    @Mock
    private UndesirablePurchaseConfigRepository configs;
    @Mock
    private AuthenticationFacade auth;

    @InjectMocks
    private ScenariosController controller;

    private MockMvc mockMvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = ContractControllerTestSupport.mockMvc(controller);
        userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void listTemplatesReturnsContractFields() throws Exception {
        ScenarioTemplate template = new ScenarioTemplate();
        template.setScenarioId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        template.setScenarioTypeCode("UNDESIRABLE_PURCHASE");
        template.setName("Нежелательные покупки");
        template.setDescription("Описание");
        template.setPublished(true);
        when(scenarioService.catalog()).thenReturn(List.of(template));

        mockMvc.perform(get("/api/v1/scenarios/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateId").value(template.getScenarioId().toString()))
                .andExpect(jsonPath("$[0].scenarioTypeCode").value("UNDESIRABLE_PURCHASE"))
                .andExpect(jsonPath("$[0].name").value("Нежелательные покупки"))
                .andExpect(jsonPath("$[0].isPublished").value(true));
    }

    @Test
    void listUserScenariosReturnsContractFields() throws Exception {
        when(auth.userId()).thenReturn(userId);
        UserScenario scenario = sampleScenario();
        when(scenarioService.list(userId)).thenReturn(List.of(scenario));
        when(configs.findByUserScenarioId(scenario.getUserScenarioId())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userScenarioId").value(scenario.getUserScenarioId().toString()))
                .andExpect(jsonPath("$[0].templateId").value(scenario.getTemplate().getScenarioId().toString()))
                .andExpect(jsonPath("$[0].debitAmount").value("200.00"))
                .andExpect(jsonPath("$[0].currency").value("RUB"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void activateScenarioReturns201() throws Exception {
        when(auth.userId()).thenReturn(userId);
        UserScenario scenario = sampleScenario();
        when(scenarioService.activate(eq(userId), any())).thenReturn(scenario);
        when(configs.findByUserScenarioId(scenario.getUserScenarioId())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","linkedAccountId":"bbbbbbbb-cccc-dddd-eeee-ffffffffffff","debitAmount":"200.00","currency":"RUB","recipientPaymentToken":"charity-token"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userScenarioId").exists());
    }

    @Test
    void updateScenarioReturns200() throws Exception {
        UUID userScenarioId = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001");
        when(auth.userId()).thenReturn(userId);
        UserScenario scenario = sampleScenario();
        when(scenarioService.update(eq(userId), eq(userScenarioId), any())).thenReturn(scenario);
        when(configs.findByUserScenarioId(scenario.getUserScenarioId())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/scenarios/{userScenarioId}", userScenarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"debitAmount":"250.00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debitAmount").value("200.00"));
    }

    @Test
    void deactivateScenarioReturns204() throws Exception {
        UUID userScenarioId = UUID.fromString("dddddddd-eeee-ffff-0000-111111111111");
        when(auth.userId()).thenReturn(userId);

        mockMvc.perform(delete("/api/v1/scenarios/{userScenarioId}", userScenarioId))
                .andExpect(status().isNoContent());

        verify(scenarioService).deactivate(userId, userScenarioId, true);
    }

    private UserScenario sampleScenario() {
        ScenarioTemplate template = new ScenarioTemplate();
        template.setScenarioId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        template.setScenarioTypeCode("UNDESIRABLE_PURCHASE");

        DebitConfig debitConfig = new DebitConfig();
        debitConfig.setSourceAccountId(UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff"));
        debitConfig.setDebitAmount(new Money(new BigDecimal("200.00"), CurrencyCode.RUB));
        debitConfig.setRecipientPaymentToken(new PaymentToken("charity-token"));

        UserScenario scenario = new UserScenario();
        scenario.setUserScenarioId(UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001"));
        scenario.setUserId(userId);
        scenario.setTemplate(template);
        scenario.setActive(true);
        scenario.setActivatedAt(Instant.parse("2026-06-01T10:00:00.000Z"));
        scenario.setDebitConfig(debitConfig);
        return scenario;
    }
}
