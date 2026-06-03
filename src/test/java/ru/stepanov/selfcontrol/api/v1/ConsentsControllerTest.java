package ru.stepanov.selfcontrol.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;
import ru.stepanov.selfcontrol.api.v1.support.ContractControllerTestSupport;
import ru.stepanov.selfcontrol.banking.*;
import ru.stepanov.selfcontrol.common.CurrencyCode;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConsentsControllerTest {

    @Mock
    private LinkedAccountRepository accounts;
    @Mock
    private AcceptanceService acceptanceService;
    @Mock
    private AuthenticationFacade auth;

    @InjectMocks
    private ConsentsController controller;

    private MockMvc mockMvc;
    private UUID userId;
    private UUID linkedAccountId;

    @BeforeEach
    void setUp() {
        mockMvc = ContractControllerTestSupport.mockMvc(controller);
        userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        linkedAccountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    }

    @Test
    void grantConsentReturns201WithContractFields() throws Exception {
        when(auth.userId()).thenReturn(userId);
        when(accounts.findById(linkedAccountId)).thenReturn(Optional.of(ownedAccount()));
        Consent consent = sampleConsent();
        when(acceptanceService.grant(eq(userId), eq(linkedAccountId), any())).thenReturn(consent);

        mockMvc.perform(post("/api/v1/accounts/{linkedAccountId}/consent", linkedAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalDebitLimit":"10000.00","maxSingleDebit":"500.00","currency":"RUB","expiresAt":"2030-01-01T00:00:00.000Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentId").value(consent.getConsentId().toString()))
                .andExpect(jsonPath("$.linkedAccountId").value(linkedAccountId.toString()))
                .andExpect(jsonPath("$.externalConsentId").value(consent.getExternalConsentId()))
                .andExpect(jsonPath("$.status").value("Active"))
                .andExpect(jsonPath("$.totalDebitLimit").value("10000.00"))
                .andExpect(jsonPath("$.maxSingleDebit").value("500.00"))
                .andExpect(jsonPath("$.currency").value("RUB"));
    }

    @Test
    void revokeConsentReturns204() throws Exception {
        when(auth.userId()).thenReturn(userId);
        when(accounts.findById(linkedAccountId)).thenReturn(Optional.of(ownedAccount()));

        mockMvc.perform(delete("/api/v1/accounts/{linkedAccountId}/consent", linkedAccountId))
                .andExpect(status().isNoContent());

        verify(acceptanceService).revokeByLinkedAccountId(userId, linkedAccountId);
    }

    @Test
    void grantConsentForUnknownAccountReturns404ErrorResponse() throws Exception {
        when(accounts.findById(linkedAccountId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/accounts/{linkedAccountId}/consent", linkedAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalDebitLimit":"10000.00","currency":"RUB"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value(ErrorCode.NOT_FOUND.name()));
    }

    private LinkedAccount ownedAccount() {
        LinkedAccount account = new LinkedAccount();
        account.setLinkedAccountId(linkedAccountId);
        account.setUserId(userId);
        return account;
    }

    private Consent sampleConsent() {
        AcceptanceLimit limit = new AcceptanceLimit();
        limit.setTotalDebitLimit(new Money(new BigDecimal("10000.00"), CurrencyCode.RUB));
        limit.setMaxSingleDebit(new Money(new BigDecimal("500.00"), CurrencyCode.RUB));

        Consent consent = new Consent();
        consent.setConsentId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        consent.setLinkedAccountId(linkedAccountId);
        consent.setExternalConsentId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        consent.setStatus(AcceptanceStatus.Active);
        consent.setAcceptanceLimit(limit);
        consent.setGrantedAt(Instant.parse("2026-06-01T10:00:00.000Z"));
        consent.setExpiresAt(Instant.parse("2030-01-01T00:00:00.000Z"));
        return consent;
    }
}
