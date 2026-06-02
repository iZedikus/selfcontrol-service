package ru.stepanov.selfcontrol.config;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiMediaTypeConfig {
    private static final String WILDCARD_MEDIA_TYPE = "*/*";
    private static final String JSON_MEDIA_TYPE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    @Bean
    OpenApiCustomizer openApiJsonMediaTypeCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            if (operation.getRequestBody() != null) {
                replaceWildcardWithJson(operation.getRequestBody().getContent());
            }
            if (operation.getResponses() != null) {
                operation.getResponses().values().stream()
                        .map(ApiResponse::getContent)
                        .forEach(this::replaceWildcardWithJson);
            }
        }));
    }

    private void replaceWildcardWithJson(Content content) {
        if (content == null || !content.containsKey(WILDCARD_MEDIA_TYPE)) {
            return;
        }
        MediaType wildcardContent = content.remove(WILDCARD_MEDIA_TYPE);
        content.putIfAbsent(JSON_MEDIA_TYPE, wildcardContent);
    }
}
