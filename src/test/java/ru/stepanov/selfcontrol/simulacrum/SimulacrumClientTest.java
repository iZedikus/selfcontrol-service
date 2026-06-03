package ru.stepanov.selfcontrol.simulacrum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.stepanov.selfcontrol.config.IsProperties;
import ru.stepanov.selfcontrol.config.JacksonConfig;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class SimulacrumClientTest {

    @Mock
    private SimulacrumApiLogService apiLogService;
    @Mock
    private SimulacrumApiLogRepository apiLogRepository;

    private MockRestServiceServer server;
    private SimulacrumClient client;
    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        RestClient.Builder restBuilder = RestClient.builder().baseUrl("http://simulacrum:8081");
        server = MockRestServiceServer.bindTo(restBuilder).build();
        RestClient restClient = restBuilder.build();
        client = new SimulacrumClient(
                restClient,
                new JacksonConfig().objectMapper(),
                apiLogService,
                apiLogRepository,
                new IsProperties("creditor-system-1")
        );
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void grantConsentPostsContractBodyAndParsesConsentId() {
        server.expect(requestTo("http://simulacrum:8081/api/v1/consents"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"accountId\":\"ACC-001\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditorSystemId\":\"creditor-system-1\"")))
                .andRespond(withSuccess("""
                        {
                          "consentId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                          "status":"Active"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.grantConsent(userId, new RegisterConsentRequest(
                "ACC-001", "10000.00", "500.00", "RUB", "BEXP", null,
                Instant.parse("2030-01-01T00:00:00.000Z")
        ));

        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", response.consentId());
        assertEquals("Active", response.status());
    }

    @Test
    void revokeConsentCallsDeleteEndpoint() {
        server.expect(requestTo("http://simulacrum:8081/api/v1/consents/consent-uuid-1"))
                .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                .andRespond(withSuccess());

        client.revokeConsent(userId, "consent-uuid-1");
    }

    @Test
    void submitDebitPostsContractBody() {
        server.expect(requestTo("http://simulacrum:8081/api/v1/payments/debit"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "consentId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                          "sourceAccountId":"ACC-001",
                          "recipientPaymentToken":"charity-token",
                          "amount":"200.00",
                          "currency":"RUB"
                        }
                        """, false))
                .andRespond(withSuccess("""
                        {
                          "transactionId":"TX-DEBIT-2026-001",
                          "status":"Pending"
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentDebitSubmitResponse response = client.submitDebit(userId, new PaymentDebitRequest(
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "ACC-001",
                "charity-token",
                "200.00",
                "RUB"
        ));

        assertEquals("TX-DEBIT-2026-001", response.transactionId());
        assertEquals("Pending", response.status());
    }

    @Test
    void getDebitStatusReturnsFailureDetailsWhenRejected() {
        server.expect(requestTo("http://simulacrum:8081/api/v1/payments/TX-DEBIT-2026-001/status"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "transactionId":"TX-DEBIT-2026-001",
                          "status":"Rejected",
                          "failureCode":"INSUFFICIENT_FUNDS",
                          "failureMessage":"Not enough balance"
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentStatusResponse response = client.getDebitStatus(userId, "TX-DEBIT-2026-001");

        assertEquals("Rejected", response.status());
        assertEquals("INSUFFICIENT_FUNDS", response.failureCode());
        assertEquals("Not enough balance", response.failureMessage());
    }

    @Test
    void getDebitStatusReturnsCompletedWithoutFailureFields() {
        server.expect(requestTo("http://simulacrum:8081/api/v1/payments/TX-OK/status"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "transactionId":"TX-OK",
                          "status":"AcceptedSettlementCompleted",
                          "failureCode":null,
                          "failureMessage":null
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentStatusResponse response = client.getDebitStatus(userId, "TX-OK");

        assertEquals(DebitStatuses.COMPLETED, response.status());
        assertNull(response.failureCode());
        assertNull(response.failureMessage());
    }
}
