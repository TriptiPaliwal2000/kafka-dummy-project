package com.trippy.notificationservice.controller;

import com.trippy.notificationservice.domain.Notification;
import com.trippy.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void listAll_returnsStoredNotifications() {
        Notification notification = new Notification(UUID.randomUUID(), 1L, "PRODUCT-1",
                "a@b.com", "Your order #1 for 2 x PRODUCT-1 is confirmed and on its way.", Instant.now());
        when(notificationService.listAll()).thenReturn(Flux.just(notification));

        webTestClient.get().uri("/api/notifications")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Notification.class)
                .hasSize(1);
    }
}
