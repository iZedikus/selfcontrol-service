package ru.stepanov.selfcontrol.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.stepanov.selfcontrol.config.JacksonConfig;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MoneyJsonSerializationTest {

    @Test
    void moneySerializesAmountAsStringWithTwoDecimals() throws Exception {
        ObjectMapper mapper = new JacksonConfig().objectMapper();

        String json = mapper.writeValueAsString(new Money(new BigDecimal("350"), CurrencyCode.RUB));
        JsonNode node = mapper.readTree(json);

        assertEquals("350.00", node.get("amount").asText());
        assertEquals("RUB", node.get("currency").asText());
        assertFalse(node.get("amount").isNumber());
    }
}
