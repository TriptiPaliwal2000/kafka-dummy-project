package com.trippy.inventoryservice.service;

import com.trippy.inventoryservice.dto.InventoryItemResponse;
import com.trippy.inventoryservice.dto.StockCheckResponse;

public interface InventoryService {

    InventoryItemResponse getProduct(String productId);

    StockCheckResponse checkStock(String productId, int quantity);

    void decrementStock(String productId, int quantity);
}
