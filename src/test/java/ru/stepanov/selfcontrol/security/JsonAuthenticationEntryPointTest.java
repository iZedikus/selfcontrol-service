package ru.stepanov.selfcontrol.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;
import ru.stepanov.selfcontrol.config.JacksonConfig;

import static org.junit.jupiter.api.Assertions.*;

class JsonAuthenticationEntryPointTest {

    private JsonAuthenticationEntryPoint entryPoint;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new JacksonConfig().objectMapper();
        entryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        response = new MockHttpServletResponse();
    }

    @Test
    void commenceReturnsUnauthorizedErrorResponse() throws Exception {
        entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("bad token"));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("application/json", response.getContentType());

        var body = new JacksonConfig().objectMapper().readTree(response.getContentAsString());
        assertEquals(401, body.get("status").asInt());
        assertEquals(ErrorCode.UNAUTHORIZED.name(), body.get("error").asText());
        assertEquals("Authentication required", body.get("message").asText());
        assertTrue(body.has("timestamp"));
    }
}
