package ru.stepanov.selfcontrol.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;
import ru.stepanov.selfcontrol.api.contract.PagedResponse;
import ru.stepanov.selfcontrol.api.contract.PageMeta;
import ru.stepanov.selfcontrol.api.contract.notification.NotificationResponse;
import ru.stepanov.selfcontrol.api.contract.notification.NotificationType;
import ru.stepanov.selfcontrol.api.v1.support.ContractControllerTestSupport;
import ru.stepanov.selfcontrol.notification.NotificationService;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationsControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthenticationFacade auth;

    @InjectMocks
    private NotificationsController controller;

    private MockMvc mockMvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = ContractControllerTestSupport.mockMvc(controller);
        userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void listNotificationsReturnsPagedContractFields() throws Exception {
        when(auth.userId()).thenReturn(userId);
        NotificationResponse notification = new NotificationResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                NotificationType.DebitCompleted,
                Map.of("executionId", "exec-1"),
                false,
                Instant.parse("2026-06-01T10:00:00.000Z")
        );
        when(notificationService.list(userId, false, 0, 20))
                .thenReturn(new PagedResponse<>(List.of(notification), new PageMeta(0, 20, 1, 1)));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notificationId").value(notification.notificationId().toString()))
                .andExpect(jsonPath("$.content[0].type").value("DebitCompleted"))
                .andExpect(jsonPath("$.content[0].isRead").value(false))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void markNotificationReadReturns204() throws Exception {
        UUID notificationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(auth.userId()).thenReturn(userId);

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isNoContent());

        verify(notificationService).markRead(userId, notificationId);
    }

    @Test
    void markNotificationReadNotFoundReturns404ErrorResponse() throws Exception {
        UUID notificationId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(auth.userId()).thenReturn(userId);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"))
                .when(notificationService).markRead(userId, notificationId);

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value(ErrorCode.NOT_FOUND.name()));
    }
}
