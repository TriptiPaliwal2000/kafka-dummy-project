package com.trippy.orderevents;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The single event published by order-service to the {@code order-events} Kafka topic when
 * an order is placed (in any of the statuses in {@link OrderStatus}).
 * <p>
 * This record is the integration contract between the three services. It intentionally
 * carries only what downstream consumers need - inventory-service uses it to decrement
 * stock, notification-service uses it to compose a message - rather than the full Order
 * aggregate, so that order-service's internal model can evolve without breaking consumers.
 *
 * @param eventId        unique id for this event, independent of the order id, so consumers
 *                        can de-duplicate deliveries (Kafka gives at-least-once delivery)
 * @param orderId        the order this event describes
 * @param productId      product being ordered
 * @param quantity       quantity requested
 * @param unitPrice      price per unit at the time the order was placed
 * @param customerEmail  used by notification-service to address the notification
 * @param status         order status decided by order-service (see {@link OrderStatus})
 * @param occurredAt     when order-service placed the order
 */
public record OrderPlacedEvent(
        UUID eventId,
        Long orderId,
        String productId,
        int quantity,
        BigDecimal unitPrice,
        String customerEmail,
        OrderStatus status,
        Instant occurredAt
) implements Serializable {

    public static OrderPlacedEvent of(Long orderId, String productId, int quantity,
                                       BigDecimal unitPrice, String customerEmail, OrderStatus status) {
        return new OrderPlacedEvent(UUID.randomUUID(), orderId, productId, quantity,
                unitPrice, customerEmail, status, Instant.now());
    }
}
