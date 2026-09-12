package com.trippy.inventoryservice.dto;

import com.trippy.inventoryservice.domain.InventoryItem;

import java.math.BigDecimal;

/**
 * JSON shape deliberately mirrors order-service's {@code StockCheckResult} field-for-field
 * (availability / currentStock / unitPrice). The two services intentionally do NOT share a
 * DTO class here - order-service only needs to agree on the wire contract, not compile
 * against inventory-service's code - but the shape is kept in sync so Jackson can map one
 * straight onto the other.
 */
public record StockCheckResponse(Availability availability, int currentStock, BigDecimal unitPrice) {

    public enum Availability {
        AVAILABLE,
        UNAVAILABLE
    }

    public static StockCheckResponse forItem(InventoryItem item, int requestedQuantity) {
        Availability availability = item.hasStockFor(requestedQuantity) ? Availability.AVAILABLE : Availability.UNAVAILABLE;
        return new StockCheckResponse(availability, item.getAvailableStock(), item.getUnitPrice());
    }
}
