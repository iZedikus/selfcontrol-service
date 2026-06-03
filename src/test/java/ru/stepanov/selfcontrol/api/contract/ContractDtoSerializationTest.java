package ru.stepanov.selfcontrol.api.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.stepanov.selfcontrol.api.contract.account.LinkedAccountResponse;
import ru.stepanov.selfcontrol.api.contract.account.LinkedAccountStatus;
import ru.stepanov.selfcontrol.api.contract.auth.AuthResponse;
import ru.stepanov.selfcontrol.api.contract.scenario.ScenarioTemplateResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractDtoSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void authResponseUsesContractFieldNames() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String json = mapper.writeValueAsString(new AuthResponse("access", "refresh", 1800, userId));
        JsonNode node = mapper.readTree(json);

        assertEquals("access", node.get("accessToken").asText());
        assertEquals("refresh", node.get("refreshToken").asText());
        assertEquals(1800, node.get("expiresIn").asInt());
        assertEquals(userId.toString(), node.get("userId").asText());
    }

    @Test
    void scenarioTemplateResponseUsesContractFieldNames() throws Exception {
        UUID templateId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String json = mapper.writeValueAsString(new ScenarioTemplateResponse(
                templateId, "UNDESIRABLE_PURCHASE", "Name", "Desc", true
        ));
        JsonNode node = mapper.readTree(json);

        assertEquals(templateId.toString(), node.get("templateId").asText());
        assertEquals("UNDESIRABLE_PURCHASE", node.get("scenarioTypeCode").asText());
        assertTrue(node.get("isPublished").asBoolean());
    }

    @Test
    void linkedAccountResponseUsesContractFieldNames() throws Exception {
        UUID linkedAccountId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Instant linkedAt = Instant.parse("2026-05-25T10:00:00.000Z");
        String json = mapper.writeValueAsString(new LinkedAccountResponse(
                linkedAccountId, null, "4321", "044525974", "Счёт", "RUB",
                LinkedAccountStatus.Active, linkedAt
        ));
        JsonNode node = mapper.readTree(json);

        assertEquals(linkedAccountId.toString(), node.get("linkedAccountId").asText());
        assertEquals("4321", node.get("maskedPan").asText());
        assertEquals("Active", node.get("status").asText());
        assertEquals("2026-05-25T10:00:00.000Z", node.get("linkedAt").asText());
    }

    @Test
    void pagedResponseSerializesContentAndMeta() throws Exception {
        var page = new PagedResponse<>(
                List.of("item"),
                new PageMeta(0, 20, 1, 1)
        );
        String json = mapper.writeValueAsString(page);
        JsonNode node = mapper.readTree(json);

        assertEquals(1, node.get("content").size());
        assertEquals(0, node.get("meta").get("page").asInt());
        assertEquals(20, node.get("meta").get("size").asInt());
        assertEquals(1, node.get("meta").get("totalElements").asLong());
        assertEquals(1, node.get("meta").get("totalPages").asInt());
    }
}
