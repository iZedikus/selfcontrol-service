package ru.stepanov.selfcontrol.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IsPropertiesTest {

    @Test
    void bindsCreditorSystemIdFromProperties() {
        var source = new MapConfigurationPropertySource(Map.of(
                "app.is.creditor-system-id", "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        ));
        IsProperties props = new Binder(source).bind("app.is", Bindable.of(IsProperties.class)).get();

        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", props.creditorSystemId());
    }

    @Test
    void usesDefaultWhenCreditorSystemIdMissing() {
        var source = new MapConfigurationPropertySource(Map.of());
        IsProperties props = new Binder(source).bind("app.is", Bindable.of(IsProperties.class)).orElse(new IsProperties(null));

        assertEquals(IsProperties.DEFAULT_CREDITOR_SYSTEM_ID, props.creditorSystemId());
    }
}
