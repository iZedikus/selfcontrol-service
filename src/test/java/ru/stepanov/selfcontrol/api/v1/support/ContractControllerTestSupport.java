package ru.stepanov.selfcontrol.api.v1.support;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.stepanov.selfcontrol.common.GlobalExceptionHandler;
import ru.stepanov.selfcontrol.config.JacksonConfig;

public final class ContractControllerTestSupport {

    private ContractControllerTestSupport() {
    }

    public static MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new JacksonConfig().objectMapper()))
                .build();
    }
}
