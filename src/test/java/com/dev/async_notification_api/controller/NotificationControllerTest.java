package com.dev.async_notification_api.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dev.async_notification_api.domain.NotificationStatus;
import com.dev.async_notification_api.domain.dto.NotificationResponseDTO;
import com.dev.async_notification_api.service.NotificationService;
import com.dev.async_notification_api.service.exception.NotificationNotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postShouldReturnAcceptedWithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationResponseDTO response = new NotificationResponseDTO(
                id,
                "user@example.com",
                "subject",
                "body",
                NotificationStatus.PENDENTE,
                LocalDateTime.now(),
                null
        );

        when(notificationService.send(any())).thenReturn(response);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "user@example.com",
                                  "subject": "subject",
                                  "body": "body"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/notifications/" + id))
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.status", is("PENDENTE")));
    }

    @Test
    void postShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "invalid-email",
                                  "subject": "",
                                  "body": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @Test
    void getByIdShouldReturnNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.findById(eq(id))).thenThrow(new NotificationNotFoundException(id));

        mockMvc.perform(get("/api/notifications/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
