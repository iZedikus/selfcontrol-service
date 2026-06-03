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
import ru.stepanov.selfcontrol.api.v1.support.ContractControllerTestSupport;
import ru.stepanov.selfcontrol.banking.*;
import ru.stepanov.selfcontrol.common.CurrencyCode;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountsControllerTest {

    @Mock
    private LinkedAccountRepository accounts;
    @Mock
    private BankingAccountLifecycleService accountLifecycle;
    @Mock
    private AuthenticationFacade auth;

    @InjectMocks
    private AccountsController controller;

    private MockMvc mockMvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = ContractControllerTestSupport.mockMvc(controller);
        userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void listAccountsReturnsContractFieldsWithoutPaymentToken() throws Exception {
        when(auth.userId()).thenReturn(userId);
        LinkedAccount linked = sampleAccount(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        when(accounts.findByUserId(userId)).thenReturn(List.of(linked));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].linkedAccountId").value(linked.getLinkedAccountId().toString()))
                .andExpect(jsonPath("$[0].displayName").value("Основной счёт"))
                .andExpect(jsonPath("$[0].bankBic").value("044525974"))
                .andExpect(jsonPath("$[0].currency").value("RUB"))
                .andExpect(jsonPath("$[0].status").value("Active"))
                .andExpect(jsonPath("$[0].paymentToken").doesNotExist());
    }

    @Test
    void linkAccountReturns201() throws Exception {
        when(auth.userId()).thenReturn(userId);
        LinkedAccount linked = sampleAccount(UUID.randomUUID());
        when(accountLifecycle.linkAccount(eq(userId), any())).thenReturn(linked);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentToken":"token-1","bankBic":"044525974","currency":"RUB","displayName":"Счёт","maskedPan":"4321"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkedAccountId").exists())
                .andExpect(jsonPath("$.paymentToken").doesNotExist());
    }

    @Test
    void unlinkAccountReturns204() throws Exception {
        UUID linkedAccountId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(auth.userId()).thenReturn(userId);

        mockMvc.perform(delete("/api/v1/accounts/{linkedAccountId}", linkedAccountId))
                .andExpect(status().isNoContent());

        verify(accountLifecycle).unlinkAccount(userId, linkedAccountId);
    }

    @Test
    void unlinkAccountWithActiveScenariosReturns409ErrorResponse() throws Exception {
        UUID linkedAccountId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(auth.userId()).thenReturn(userId);
        when(accountLifecycle.unlinkAccount(userId, linkedAccountId))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Linked account has active scenarios"));

        mockMvc.perform(delete("/api/v1/accounts/{linkedAccountId}", linkedAccountId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value(ErrorCode.CONFLICT.name()))
                .andExpect(jsonPath("$.message").value("Linked account has active scenarios"));
    }

    private LinkedAccount sampleAccount(UUID id) {
        LinkedAccount account = new LinkedAccount();
        account.setLinkedAccountId(id);
        account.setUserId(userId);
        account.setDisplayName("Основной счёт");
        account.setMaskedPAN("4321");
        account.setBankBIC(new BankBIC("044525974"));
        account.setCurrency(CurrencyCode.RUB);
        account.setStatus(LinkedAccountStatus.Active);
        account.setLinkedAt(Instant.parse("2026-06-01T10:00:00.000Z"));
        return account;
    }
}
