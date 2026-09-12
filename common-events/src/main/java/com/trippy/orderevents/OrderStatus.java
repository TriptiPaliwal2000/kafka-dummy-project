package com.trippy.orderevents;

/**
 * Shared order lifecycle status, understood by order-service (writer) and by
 * inventory-service / notification-service (readers of the event that carries it).
 */
public enum OrderStatus {

    /** Inventory was checked synchronously and stock was confirmed available. */
    CONFIRMED,

    /**
     * Inventory could not be verified because the call to inventory-service failed or the
     * Resilience4j circuit breaker was open. The order is still accepted (we don't want a
     * dependency outage to block checkout) but flagged for follow-up - this is the
     * "graceful degradation" path.
     */
    PENDING_VERIFICATION,

    /** Inventory was checked and there was not enough stock. */
    REJECTED_OUT_OF_STOCK
}
