package ru.stepanov.selfcontrol.scenario;

import ru.stepanov.selfcontrol.banking.LinkedAccount;
import ru.stepanov.selfcontrol.banking.LinkedAccountRepository;
import ru.stepanov.selfcontrol.rabbit.DebitConfigDto;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncAction;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncMessage;
import ru.stepanov.selfcontrol.rabbit.ProfileSyncPublisher;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchaseConfig;
import ru.stepanov.selfcontrol.undesirable.UndesirablePurchasePlugin;

import java.time.Instant;
import java.util.UUID;

/**
 * Публикация {@link ProfileSyncMessage} для ORACLE (общая логика ScenarioService / AcceptanceService).
 */
public final class ScenarioProfileSyncSupport {

    private ScenarioProfileSyncSupport() {
    }

    public static void publish(ProfileSyncPublisher publisher,
                               UndesirablePurchasePlugin plugin,
                               LinkedAccountRepository accounts,
                               UserScenario scenario,
                               UndesirablePurchaseConfig config,
                               ProfileSyncAction action) {
        LinkedAccount account = accounts.findById(scenario.getDebitConfig().getSourceAccountId()).orElse(null);
        DebitConfigDto debitConfig = new DebitConfigDto(
                scenario.getDebitConfig().getDebitAmount().getAmount().setScale(2).toPlainString(),
                scenario.getDebitConfig().getDebitAmount().getCurrency().name(),
                scenario.getDebitConfig().getRecipientPaymentToken().getValue(),
                scenario.getDebitConfig().getAcceptanceId(),
                scenario.getDebitConfig().getSourceAccountId()
        );
        publisher.publish(new ProfileSyncMessage(
                UUID.randomUUID(),
                Instant.now(),
                action,
                scenario.getUserId(),
                scenario.getUserScenarioId(),
                UndesirablePurchasePlugin.SCENARIO_TYPE_CODE,
                account == null || account.getPaymentToken() == null ? null : account.getPaymentToken().getValue(),
                account == null || account.getBankBIC() == null ? null : account.getBankBIC().getValue(),
                config.getVersion(),
                plugin.buildOracleRules(config),
                debitConfig
        ));
    }
}
