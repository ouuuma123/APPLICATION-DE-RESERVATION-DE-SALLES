package org.example.dto;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String type,
        String message,
        boolean read,
        LocalDateTime createdAt,
        Long bookingId) {
}
