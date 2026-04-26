package com.dev.async_notification_api.domain.dto;

import com.dev.async_notification_api.domain.NotificationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseDTO(
        UUID id,
        String recipient,
        String subject,
        String body,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
