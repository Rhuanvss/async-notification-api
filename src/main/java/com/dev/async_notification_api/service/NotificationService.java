package com.dev.async_notification_api.service;

import com.dev.async_notification_api.config.RabbitMQConfig;
import com.dev.async_notification_api.domain.Notification;
import com.dev.async_notification_api.domain.dto.NotificationRequestDTO;
import com.dev.async_notification_api.domain.dto.NotificationResponseDTO;
import com.dev.async_notification_api.repository.NotificationRepository;
import com.dev.async_notification_api.service.exception.NotificationNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;

    public NotificationService(NotificationRepository notificationRepository, RabbitTemplate rabbitTemplate) {
        this.notificationRepository = notificationRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public NotificationResponseDTO send(NotificationRequestDTO request) {
        Notification notification = new Notification();
        notification.setRecipient(request.recipient());
        notification.setSubject(request.subject());
        notification.setBody(request.body());

        Notification saved = notificationRepository.save(notification);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, saved.getId().toString());

        return toResponse(saved);
    }

    public NotificationResponseDTO findById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        return toResponse(notification);
    }

    public List<NotificationResponseDTO> findAll() {
        return notificationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponseDTO toResponse(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getProcessedAt()
        );
    }
}
