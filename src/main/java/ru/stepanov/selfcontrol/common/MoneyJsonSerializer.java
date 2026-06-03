package ru.stepanov.selfcontrol.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.RoundingMode;

/**
 * Сериализация {@link Money} для REST API: сумма — строка с двумя знаками, валюта — ISO 4217.
 */
public class MoneyJsonSerializer extends JsonSerializer<Money> {

    @Override
    public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        if (value.getAmount() != null) {
            gen.writeStringField("amount", value.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
        if (value.getCurrency() != null) {
            gen.writeStringField("currency", value.getCurrency().name());
        }
        gen.writeEndObject();
    }
}
