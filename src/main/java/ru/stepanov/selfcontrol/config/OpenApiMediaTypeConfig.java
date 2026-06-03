package ru.stepanov.selfcontrol.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiMediaTypeConfig {
    static final String BEARER_AUTH_SCHEME = "bearerAuth";
    private static final String WILDCARD_MEDIA_TYPE = "*/*";
    private static final String JSON_MEDIA_TYPE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    @Bean
    OpenApiCustomizer openApiJsonMediaTypeCustomizer() {
        return openApi -> {
            addBearerSecurity(openApi);
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> {
                if (operation.getRequestBody() != null) {
                    replaceWildcardWithJson(operation.getRequestBody().getContent());
                }
                if (operation.getResponses() != null) {
                    operation.getResponses().values().stream()
                            .map(ApiResponse::getContent)
                            .forEach(this::replaceWildcardWithJson);
                }
                if (isPublicEndpoint(path, pathItem, operation)) {
                    operation.setSecurity(List.of());
                }
            }));
        };
    }

    private void addBearerSecurity(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        components.addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
        openApi.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }

    private boolean isPublicEndpoint(String path, PathItem pathItem, Operation operation) {
        return path.startsWith("/api/v1/auth/")
                || ("/api/v1/scenarios/templates".equals(path) && pathItem.getGet() == operation);
    }

    private void replaceWildcardWithJson(Content content) {
        if (content == null || !content.containsKey(WILDCARD_MEDIA_TYPE)) {
            return;
        }
        MediaType wildcardContent = content.remove(WILDCARD_MEDIA_TYPE);
        content.putIfAbsent(JSON_MEDIA_TYPE, wildcardContent);
    }
}
