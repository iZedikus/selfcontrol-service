package ru.stepanov.selfcontrol.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.stepanov.selfcontrol.identity.UserRole;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BearerTokenFilterTest {
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterAcceptsLowercaseBearerSchemeAndUsesCanonicalAdminAuthority() throws Exception {
        TokenService tokenService = mock(TokenService.class);
        when(tokenService.parse("admin-token")).thenReturn(Optional.of(new TokenService.AuthUser(UUID.randomUUID(), UserRole.Admin)));
        BearerTokenFilter filter = new BearerTokenFilter(tokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.addHeader("Authorization", "bearer admin-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        verify(tokenService).parse("admin-token");
    }
}
