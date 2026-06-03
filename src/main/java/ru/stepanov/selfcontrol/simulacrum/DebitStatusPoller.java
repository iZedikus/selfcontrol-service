package ru.stepanov.selfcontrol.simulacrum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Polling финального статуса списания после POST /api/v1/payments/debit (MVP: до 5 попыток, пауза 1 с).
 */
@Component
public class DebitStatusPoller {

    static final int MAX_ATTEMPTS = 5;
    static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final SimulacrumClient simulacrum;
    private final Sleeper sleeper;

    @Autowired
    public DebitStatusPoller(SimulacrumClient simulacrum) {
        this(simulacrum, duration -> {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while polling debit status", e);
            }
        });
    }

    static DebitStatusPoller createForTesting(SimulacrumClient simulacrum, Sleeper sleeper) {
        return new DebitStatusPoller(simulacrum, sleeper);
    }

    private DebitStatusPoller(SimulacrumClient simulacrum, Sleeper sleeper) {
        this.simulacrum = simulacrum;
        this.sleeper = sleeper;
    }

    public PaymentStatusResponse pollUntilFinal(UUID userId, String transactionId) {
        PaymentStatusResponse last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            last = simulacrum.getDebitStatus(userId, transactionId);
            if (last.isFinal()) {
                return last;
            }
            if (attempt < MAX_ATTEMPTS - 1) {
                sleeper.sleep(POLL_INTERVAL);
            }
        }
        throw new DebitStatusPollingException(
                "Debit status polling timed out after " + MAX_ATTEMPTS + " attempts for transaction " + transactionId,
                last
        );
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration);
    }
}
