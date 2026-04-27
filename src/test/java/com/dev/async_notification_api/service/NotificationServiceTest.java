package com.dev.async_notification_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dev.async_notification_api.config.RabbitMQConfig;
import com.dev.async_notification_api.domain.Notification;
import com.dev.async_notification_api.domain.NotificationStatus;
import com.dev.async_notification_api.domain.dto.NotificationRequestDTO;
import com.dev.async_notification_api.repository.NotificationRepository;
import com.dev.async_notification_api.service.exception.NotificationNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendShouldPersistAndPublishNotification() {
        NotificationRequestDTO request = new NotificationRequestDTO("user@example.com", "Hello", "Message");
        UUID id = UUID.randomUUID();

        Notification persisted = new Notification();
        persisted.setId(id);
        persisted.setRecipient(request.recipient());
        persisted.setSubject(request.subject());
        persisted.setBody(request.body());
        persisted.setStatus(NotificationStatus.PENDENTE);
        persisted.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(persisted);

        var response = notificationService.send(request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(NotificationStatus.PENDENTE);
        verify(notificationRepository).save(any(Notification.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(id.toString()));
    }

    @Test
    void findByIdShouldThrowWhenNotificationDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.findById(id))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
