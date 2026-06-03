package ru.stepanov.selfcontrol.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.common.CurrencyCode;
import ru.stepanov.selfcontrol.common.Money;
import ru.stepanov.selfcontrol.notification.NotificationService;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncAction;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncMessage;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncPublisher;
import ru.stepanov.selfcontrol.scenario.DebitConfig;
import ru.stepanov.selfcontrol.scenario.UserScenario;
import ru.stepanov.selfcontrol.scenario.UserScenarioRepository;
import ru.stepanov.selfcontrol.simulacrum.SimulacrumClient;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchaseConfig;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchaseConfigRepository;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchasePlugin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptanceServiceRevokeTest {

    @Mock
    private ConsentRepository consents;
    @Mock
    private LinkedAccountRepository accounts;
    @Mock
    private UserScenarioRepository userScenarios;
    @Mock
    private UndesirablePurchaseConfigRepository scenarioConfigs;
    @Mock
    private SimulacrumClient simulacrum;
    @Mock
    private ProfileSyncPublisher profileSyncPublisher;
    @Mock
    private UndesirablePurchasePlugin undesirablePurchasePlugin;
    @Mock
    private NotificationService notifications;
    @Mock
    private AuditService audit;

    private AcceptanceService service;

    @BeforeEach
    void setUp() {
        service = new AcceptanceService(
                consents, accounts, userScenarios, scenarioConfigs,
                simulacrum, profileSyncPublisher, undesirablePurchasePlugin, notifications, audit);
    }

    @Test
    void revokeByLinkedAccountIdPublishesTerminateForActiveScenarios() {
        UUID userId = UUID.randomUUID();
        UUID linkedAccountId = UUID.randomUUID();
        UUID scenarioId = UUID.randomUUID();

        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID());
        consent.setUserId(userId);
        consent.setLinkedAccountId(linkedAccountId);
        consent.setExternalConsentId("ext-consent-1");
        consent.setStatus(AcceptanceStatus.Active);

        UserScenario scenario = new UserScenario();
        scenario.setUserScenarioId(scenarioId);
        scenario.setUserId(userId);
        scenario.setActive(true);
        DebitConfig debitConfig = new DebitConfig();
        debitConfig.setSourceAccountId(linkedAccountId);
        debitConfig.setDebitAmount(new Money(new BigDecimal("100.00"), CurrencyCode.RUB));
        debitConfig.setRecipientPaymentToken(new PaymentToken("recipient-1"));
        debitConfig.setAcceptanceId(consent.getConsentId());
        scenario.setDebitConfig(debitConfig);

        UndesirablePurchaseConfig config = new UndesirablePurchaseConfig();
        config.setUserScenarioId(scenarioId);
        config.setVersion(1);

        LinkedAccount account = new LinkedAccount();
        account.setLinkedAccountId(linkedAccountId);
        account.setPaymentToken(new PaymentToken("pay-token-1"));

        when(consents.findByLinkedAccountId(linkedAccountId)).thenReturn(Optional.of(consent));
        when(consents.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userScenarios.findByActiveTrueAndDebitConfig_SourceAccountId(linkedAccountId))
                .thenReturn(List.of(scenario));
        when(scenarioConfigs.findByUserScenarioId(scenarioId)).thenReturn(Optional.of(config));
        when(accounts.findById(linkedAccountId)).thenReturn(Optional.of(account));
        when(undesirablePurchasePlugin.buildOracleRules(config)).thenReturn(List.of());

        service.revokeByLinkedAccountId(userId, linkedAccountId);

        verify(simulacrum).revokeConsent(userId, "ext-consent-1");
        ArgumentCaptor<ProfileSyncMessage> captor = ArgumentCaptor.forClass(ProfileSyncMessage.class);
        verify(profileSyncPublisher).publish(captor.capture());
        assertEquals(ProfileSyncAction.TERMINATE, captor.getValue().action());
        assertEquals(scenarioId, captor.getValue().externalUserScenarioId());
        verify(notifications).notifyConsentRevoked(userId, consent.getConsentId(), linkedAccountId);
    }
}
