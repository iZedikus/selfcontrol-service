package ru.stepanov.selfcontrol.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;
import ru.stepanov.selfcontrol.api.contract.auth.AuthResponse;
import ru.stepanov.selfcontrol.api.v1.support.ContractControllerTestSupport;
import ru.stepanov.selfcontrol.identity.AuthController;
import ru.stepanov.selfcontrol.identity.AuthService;
import ru.stepanov.selfcontrol.security.TokenService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ContractControllerTestSupport.mockMvc(controller);
    }

    @Test
    void registerReturns201WithContractFields() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        when(authService.register(any())).thenReturn(
                new AuthResponse("access-token", "refresh-token", TokenService.ACCESS_TOKEN_EXPIRES_IN_SECONDS, userId));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@test.local","password":"password1","firstName":"Ivan","lastName":"Ivanov","phoneNumber":"+79001234567"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(TokenService.ACCESS_TOKEN_EXPIRES_IN_SECONDS))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void loginReturns200WithContractFields() throws Exception {
        UUID userId = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");
        when(authService.login(any())).thenReturn(
                new AuthResponse("access-token", "refresh-token", TokenService.ACCESS_TOKEN_EXPIRES_IN_SECONDS, userId));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@test.local","password":"password1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void refreshReturns200WithContractFields() throws Exception {
        UUID userId = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001");
        when(authService.refresh(any())).thenReturn(
                new AuthResponse("new-access", "new-refresh", TokenService.ACCESS_TOKEN_EXPIRES_IN_SECONDS, userId));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"old-refresh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void loginInvalidCredentialsReturns401ErrorResponse() throws Exception {
        when(authService.login(any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@test.local","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value(ErrorCode.UNAUTHORIZED.name()))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
