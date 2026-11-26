package org.example.controller;

import org.example.dto.NotificationDTO;
import org.example.model.Notification;
import org.example.model.User;
import org.example.service.NotificationService;
import org.example.service.UserServiceInterface;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final UserServiceInterface userService;

    public NotificationRestController(
            NotificationService notificationService, UserServiceInterface userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public List<NotificationDTO> getMyNotifications(Authentication authentication) {
        User user = currentUser(authentication);
        return notificationService.getForUser(user.getId()).stream()
                .map(this::toDTO)
                .toList();
    }

    @PatchMapping("/{id}/read")
    public NotificationDTO markAsRead(@PathVariable Long id, Authentication authentication) {
        return toDTO(notificationService.markAsRead(id, currentUser(authentication).getId()));
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getBooking() == null ? null : notification.getBooking().getId());
    }
}
