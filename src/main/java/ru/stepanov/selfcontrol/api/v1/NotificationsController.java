package ru.stepanov.selfcontrol.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.stepanov.selfcontrol.api.contract.PagedResponse;
import ru.stepanov.selfcontrol.api.contract.notification.NotificationResponse;
import ru.stepanov.selfcontrol.notification.NotificationService;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationsController {

    private final NotificationService notificationService;
    private final AuthenticationFacade auth;

    public NotificationsController(NotificationService notificationService, AuthenticationFacade auth) {
        this.notificationService = notificationService;
        this.auth = auth;
    }

    @GetMapping
    PagedResponse<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(auth.userId(), unreadOnly, page, size);
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@PathVariable UUID notificationId) {
        notificationService.markRead(auth.userId(), notificationId);
    }
}
