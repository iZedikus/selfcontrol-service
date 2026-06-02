package ru.stepanov.selfcontrol.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {
    @Test
    void corsConfigurationAllowsAuthorizationPreflightForProtectedApi() {
        SecurityConfig config = new SecurityConfig();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/profile");

        CorsConfiguration cors = config.corsConfigurationSource().getCorsConfiguration(request);

        assertNotNull(cors);
        assertEquals(List.of("*"), cors.getAllowedOriginPatterns());
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
    }
}
