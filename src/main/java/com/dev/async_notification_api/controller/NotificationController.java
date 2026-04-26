package com.dev.async_notification_api.controller;

import com.dev.async_notification_api.domain.dto.NotificationRequestDTO;
import com.dev.async_notification_api.domain.dto.NotificationResponseDTO;
import com.dev.async_notification_api.service.NotificationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponseDTO> send(@Valid @RequestBody NotificationRequestDTO request) {
        NotificationResponseDTO response = notificationService.send(request);
        return ResponseEntity.accepted()
                .location(URI.create("/api/notifications/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> findAll() {
        return ResponseEntity.ok(notificationService.findAll());
    }
}
