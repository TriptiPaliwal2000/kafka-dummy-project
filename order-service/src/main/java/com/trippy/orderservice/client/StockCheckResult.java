package com.trippy.orderservice.client;

import java.math.BigDecimal;

/**
 * Outcome of asking inventory-service whether it can fulfil a quantity of a product.
 * {@link Availability#UNKNOWN} is the degraded-mode result returned by the circuit breaker
 * fallback when inventory-service can't be reached in time - see InventoryClient.
 */
public record StockCheckResult(Availability availability, int currentStock, BigDecimal unitPrice) {

    public enum Availability {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN
    }

    public static StockCheckResult unknown() {
        return new StockCheckResult(Availability.UNKNOWN, -1, null);
    }
}
