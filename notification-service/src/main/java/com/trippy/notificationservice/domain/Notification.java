package com.trippy.notificationservice.domain;

import com.trippy.orderevents.OrderPlacedEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * What notification-service actually "sends" (here: logs + stores; swap in an email/SMS
 * provider call and nothing else about this record needs to change).
 */
public record Notification(
        UUID id,
        Long orderId,
        String productId,
        String customerEmail,
        String message,
        Instant sentAt
) {

    public static Notification from(OrderPlacedEvent event) {
        String message = switch (event.status()) {
            case CONFIRMED -> "Your order #%d for %d x %s is confirmed and on its way."
                    .formatted(event.orderId(), event.quantity(), event.productId());
            case PENDING_VERIFICATION -> "Your order #%d has been received and is being verified - "
                    .formatted(event.orderId()) + "we'll email you again shortly.";
            case REJECTED_OUT_OF_STOCK -> "We're sorry - order #%d for %s is currently out of stock."
                    .formatted(event.orderId(), event.productId());
        };
        return new Notification(UUID.randomUUID(), event.orderId(), event.productId(),
                event.customerEmail(), message, Instant.now());
    }
}
