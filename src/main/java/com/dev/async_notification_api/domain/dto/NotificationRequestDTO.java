package com.dev.async_notification_api.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationRequestDTO(
        @NotBlank @Email String recipient,
        @NotBlank String subject,
        @NotBlank String body
) {
}
