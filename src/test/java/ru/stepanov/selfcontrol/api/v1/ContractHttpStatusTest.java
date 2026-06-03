package ru.stepanov.selfcontrol.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.stepanov.selfcontrol.banking.*;
import ru.stepanov.selfcontrol.common.GlobalExceptionHandler;
import ru.stepanov.selfcontrol.config.JacksonConfig;
import ru.stepanov.selfcontrol.identity.AuthController;
import ru.stepanov.selfcontrol.identity.AuthService;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContractHttpStatusTest {

    @Mock
    private AuthService authService;
    @Mock
    private LinkedAccountRepository accounts;
    @Mock
    private BankingAccountLifecycleService accountLifecycle;
    @Mock
    private AcceptanceService acceptanceService;
    @Mock
    private AuthenticationFacade auth;

    @InjectMocks
    private AuthController authController;
    @InjectMocks
    private AccountsController accountsController;
    @InjectMocks
    private ConsentsController consentsController;

    private MockMvc authMvc;
    private MockMvc accountsMvc;
    private MockMvc consentsMvc;

    @BeforeEach
    void setUp() {
        var converter = new MappingJackson2HttpMessageConverter(new JacksonConfig().objectMapper());

        authMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
        accountsMvc = MockMvcBuilders.standaloneSetup(accountsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
        consentsMvc = MockMvcBuilders.standaloneSetup(consentsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void registerReturns201() throws Exception {
        when(authService.register(any())).thenReturn(
                new AuthService.AuthResponse("a", "r", UUID.randomUUID(), "User", "Active"));

        authMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u@test.local","password":"password1","phoneNumber":"+79001234567"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void linkAccountReturns201() throws Exception {
        UUID userId = UUID.randomUUID();
        when(auth.userId()).thenReturn(userId);
        LinkedAccount linked = new LinkedAccount();
        linked.setLinkedAccountId(UUID.randomUUID());
        when(accountLifecycle.linkAccount(userId, "token-1")).thenReturn(linked);

        accountsMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentToken":"token-1","bankBic":"044525974","currency":"RUB","displayName":"Счёт"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void unlinkAccountReturns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID linkedAccountId = UUID.randomUUID();
        when(auth.userId()).thenReturn(userId);

        accountsMvc.perform(delete("/api/v1/accounts/{id}", linkedAccountId))
                .andExpect(status().isNoContent());

        verify(accountLifecycle).unlinkAccount(userId, linkedAccountId);
    }

    @Test
    void grantConsentReturns201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID linkedAccountId = UUID.randomUUID();
        when(auth.userId()).thenReturn(userId);
        LinkedAccount account = new LinkedAccount();
        account.setLinkedAccountId(linkedAccountId);
        account.setUserId(userId);
        when(accounts.findById(linkedAccountId)).thenReturn(java.util.Optional.of(account));

        Acceptance acceptance = new Acceptance();
        acceptance.setAcceptanceId(UUID.randomUUID());
        acceptance.setExternalConsentId(UUID.randomUUID().toString());
        acceptance.setStatus(AcceptanceStatus.Active);
        acceptance.getLinkedAccounts().add(account);
        when(acceptanceService.grant(any(), any())).thenReturn(acceptance);

        consentsMvc.perform(post("/api/v1/accounts/{id}/consent", linkedAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalDebitLimit":"10000.00","currency":"RUB","expiresAt":"2030-01-01T00:00:00.000Z"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void revokeConsentReturns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID linkedAccountId = UUID.randomUUID();
        UUID acceptanceId = UUID.randomUUID();
        when(auth.userId()).thenReturn(userId);
        LinkedAccount account = new LinkedAccount();
        account.setLinkedAccountId(linkedAccountId);
        account.setUserId(userId);
        Acceptance acceptance = new Acceptance();
        acceptance.setAcceptanceId(acceptanceId);
        account.setAcceptance(acceptance);
        when(accounts.findById(linkedAccountId)).thenReturn(java.util.Optional.of(account));

        consentsMvc.perform(delete("/api/v1/accounts/{id}/consent", linkedAccountId))
                .andExpect(status().isNoContent());

        verify(acceptanceService).revoke(userId, acceptanceId);
    }
}
