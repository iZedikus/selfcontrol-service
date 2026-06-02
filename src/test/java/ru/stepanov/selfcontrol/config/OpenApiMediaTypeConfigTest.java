package ru.stepanov.selfcontrol.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiMediaTypeConfigTest {
    private final OpenApiMediaTypeConfig config = new OpenApiMediaTypeConfig();

    @Test
    void customizerAddsBearerAuthForProtectedSwaggerOperations() {
        Operation protectedOperation = new Operation();
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/v1/profile", new PathItem().get(protectedOperation)));

        config.openApiJsonMediaTypeCustomizer().customise(openApi);

        assertTrue(openApi.getComponents().getSecuritySchemes().containsKey(OpenApiMediaTypeConfig.BEARER_AUTH_SCHEME));
        assertEquals("bearer", openApi.getComponents().getSecuritySchemes().get(OpenApiMediaTypeConfig.BEARER_AUTH_SCHEME).getScheme());
        assertEquals(OpenApiMediaTypeConfig.BEARER_AUTH_SCHEME, openApi.getSecurity().getFirst().keySet().iterator().next());
        assertNull(protectedOperation.getSecurity());
    }

    @Test
    void customizerKeepsPublicSwaggerOperationsWithoutBearerAuthRequirement() {
        Operation authOperation = new Operation();
        Operation publicTemplateOperation = new Operation();
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/v1/auth/login", new PathItem().post(authOperation))
                .addPathItem("/api/v1/scenario-templates", new PathItem().get(publicTemplateOperation)));

        config.openApiJsonMediaTypeCustomizer().customise(openApi);

        assertNotNull(authOperation.getSecurity());
        assertTrue(authOperation.getSecurity().isEmpty());
        assertNotNull(publicTemplateOperation.getSecurity());
        assertTrue(publicTemplateOperation.getSecurity().isEmpty());
    }

    @Test
    void customizerReplacesWildcardMediaTypesWithJson() {
        Operation operation = new Operation()
                .requestBody(new RequestBody().content(new Content().addMediaType("*/*", new MediaType())))
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse()
                        .content(new Content().addMediaType("*/*", new MediaType()))));
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/v1/profile", new PathItem().put(operation)));

        config.openApiJsonMediaTypeCustomizer().customise(openApi);

        assertFalse(operation.getRequestBody().getContent().containsKey("*/*"));
        assertTrue(operation.getRequestBody().getContent().containsKey("application/json"));
        assertFalse(operation.getResponses().get("200").getContent().containsKey("*/*"));
        assertTrue(operation.getResponses().get("200").getContent().containsKey("application/json"));
    }
}
