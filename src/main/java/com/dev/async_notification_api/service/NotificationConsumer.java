package com.dev.async_notification_api.service;

import com.dev.async_notification_api.config.RabbitMQConfig;
import com.dev.async_notification_api.domain.Notification;
import com.dev.async_notification_api.domain.NotificationStatus;
import com.dev.async_notification_api.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRepository notificationRepository;

    public NotificationConsumer(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consume(String notificationId) {
        UUID id = UUID.fromString(notificationId);
        notificationRepository.findById(id).ifPresent(notification -> {
            try {
                simulateEmailSending(notification);
                notification.setStatus(NotificationStatus.ENVIADO);
            } catch (Exception ex) {
                LOGGER.error("Failed to process notification {}", id, ex);
                notification.setStatus(NotificationStatus.FALHA);
            }

            notification.setProcessedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    private void simulateEmailSending(Notification notification) throws InterruptedException {
        LOGGER.info("Sending e-mail to {} with subject '{}'", notification.getRecipient(), notification.getSubject());
        Thread.sleep(1000);
        LOGGER.info("E-mail successfully sent to {}", notification.getRecipient());
    }
}
