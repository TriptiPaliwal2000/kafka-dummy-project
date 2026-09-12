package com.trippy.inventoryservice.dto;

import com.trippy.inventoryservice.domain.InventoryItem;

import java.math.BigDecimal;

public record InventoryItemResponse(String productId, String productName, int availableStock, BigDecimal unitPrice) {

    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(item.getProductId(), item.getProductName(),
                item.getAvailableStock(), item.getUnitPrice());
    }
}
