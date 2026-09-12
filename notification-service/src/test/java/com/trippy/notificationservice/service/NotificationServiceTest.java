package com.trippy.notificationservice.service;

import com.trippy.notificationservice.domain.Notification;
import com.trippy.notificationservice.repository.InMemoryNotificationStore;
import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.orderevents.OrderStatus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactive unit test using StepVerifier (the Mono/Flux equivalent of asserting a return
 * value) rather than blocking on the publishers - blocking in a test for reactive code tends
 * to hide the exact bugs (timing, multiple subscription, backpressure) that reactive code is
 * prone to.
 */
class NotificationServiceTest {

    private final InMemoryNotificationStore store = new InMemoryNotificationStore();
    private final NotificationService notificationService = new NotificationService(store);

    @Test
    void handle_confirmedOrder_producesConfirmationMessageAndStoresIt() {
        OrderPlacedEvent event = OrderPlacedEvent.of(10L, "PRODUCT-1", 2, BigDecimal.valueOf(19.99),
                "buyer@example.com", OrderStatus.CONFIRMED);

        StepVerifier.create(notificationService.handle(event))
                .assertNext(notification -> {
                    assertThat(notification.orderId()).isEqualTo(10L);
                    assertThat(notification.message()).contains("confirmed");
                })
                .verifyComplete();

        StepVerifier.create(notificationService.listAll())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void handle_rejectedOrder_producesOutOfStockMessage() {
        OrderPlacedEvent event = OrderPlacedEvent.of(11L, "PRODUCT-2", 1, BigDecimal.valueOf(9.99),
                "buyer@example.com", OrderStatus.REJECTED_OUT_OF_STOCK);

        StepVerifier.create(notificationService.handle(event))
                .assertNext(notification -> assertThat(notification.message()).contains("out of stock"))
                .verifyComplete();
    }

    @Test
    void handle_pendingVerificationOrder_producesVerificationMessage() {
        OrderPlacedEvent event = OrderPlacedEvent.of(12L, "PRODUCT-3", 1, null,
                "buyer@example.com", OrderStatus.PENDING_VERIFICATION);

        StepVerifier.create(notificationService.handle(event))
                .assertNext(notification -> assertThat(notification.message()).contains("being verified"))
                .verifyComplete();
    }

    @Test
    void listAll_returnsNewestFirst() {
        OrderPlacedEvent first = OrderPlacedEvent.of(1L, "P", 1, BigDecimal.ONE, "a@b.com", OrderStatus.CONFIRMED);
        OrderPlacedEvent second = OrderPlacedEvent.of(2L, "P", 1, BigDecimal.ONE, "a@b.com", OrderStatus.CONFIRMED);

        notificationService.handle(first).block();
        notificationService.handle(second).block();

        StepVerifier.create(notificationService.listAll())
                .assertNext((Notification n) -> assertThat(n.orderId()).isEqualTo(2L))
                .assertNext((Notification n) -> assertThat(n.orderId()).isEqualTo(1L))
                .verifyComplete();
    }
}
