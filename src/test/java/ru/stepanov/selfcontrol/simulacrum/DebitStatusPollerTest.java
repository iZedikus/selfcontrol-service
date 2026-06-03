package ru.stepanov.selfcontrol.simulacrum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitStatusPollerTest {

    @Mock
    private SimulacrumClient simulacrum;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void retriesWhilePendingThenReturnsCompleted() {
        AtomicInteger calls = new AtomicInteger();
        when(simulacrum.getDebitStatus(eq(userId), eq("TX-1"))).thenAnswer(invocation -> {
            return switch (calls.getAndIncrement()) {
                case 0 -> new PaymentStatusResponse("TX-1", DebitStatuses.PENDING, null, null);
                case 1 -> new PaymentStatusResponse("TX-1", DebitStatuses.IN_PROCESS, null, null);
                default -> new PaymentStatusResponse("TX-1", DebitStatuses.COMPLETED, null, null);
            };
        });

        DebitStatusPoller poller = new DebitStatusPoller(simulacrum, duration -> { });
        PaymentStatusResponse result = poller.pollUntilFinal(userId, "TX-1");

        assertEquals(DebitStatuses.COMPLETED, result.status());
        verify(simulacrum, times(3)).getDebitStatus(userId, "TX-1");
    }

    @Test
    void returnsRejectedWithFailureDetails() {
        when(simulacrum.getDebitStatus(userId, "TX-2"))
                .thenReturn(new PaymentStatusResponse("TX-2", DebitStatuses.REJECTED, "INSUFFICIENT_FUNDS", "Low balance"));

        DebitStatusPoller poller = new DebitStatusPoller(simulacrum, duration -> { });
        PaymentStatusResponse result = poller.pollUntilFinal(userId, "TX-2");

        assertEquals(DebitStatuses.REJECTED, result.status());
        assertEquals("INSUFFICIENT_FUNDS", result.failureCode());
        assertEquals("Low balance", result.failureMessage());
        verify(simulacrum, times(1)).getDebitStatus(userId, "TX-2");
    }

    @Test
    void throwsWhenStillPendingAfterMaxAttempts() {
        when(simulacrum.getDebitStatus(userId, "TX-3"))
                .thenReturn(new PaymentStatusResponse("TX-3", DebitStatuses.PENDING, null, null));

        DebitStatusPoller poller = new DebitStatusPoller(simulacrum, duration -> { });

        DebitStatusPollingException ex = assertThrows(DebitStatusPollingException.class,
                () -> poller.pollUntilFinal(userId, "TX-3"));

        assertTrue(ex.getMessage().contains("timed out"));
        assertEquals(DebitStatuses.PENDING, ex.getLastStatus().status());
        verify(simulacrum, times(DebitStatusPoller.MAX_ATTEMPTS)).getDebitStatus(userId, "TX-3");
    }

    @Test
    void sleepsBetweenAttempts() {
        when(simulacrum.getDebitStatus(userId, "TX-4"))
                .thenReturn(new PaymentStatusResponse("TX-4", DebitStatuses.PENDING, null, null))
                .thenReturn(new PaymentStatusResponse("TX-4", DebitStatuses.COMPLETED, null, null));

        AtomicInteger sleeps = new AtomicInteger();
        DebitStatusPoller poller = new DebitStatusPoller(simulacrum, duration -> sleeps.incrementAndGet());

        poller.pollUntilFinal(userId, "TX-4");

        assertEquals(1, sleeps.get());
    }
}
