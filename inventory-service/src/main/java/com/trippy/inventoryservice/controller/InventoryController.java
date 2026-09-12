package com.trippy.inventoryservice.controller;

import com.trippy.inventoryservice.dto.InventoryItemResponse;
import com.trippy.inventoryservice.dto.StockCheckResponse;
import com.trippy.inventoryservice.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public InventoryItemResponse getProduct(@PathVariable String productId) {
        return inventoryService.getProduct(productId);
    }

    /** Called synchronously by order-service (via InventoryClient) before confirming an order. */
    @GetMapping("/{productId}/check")
    public StockCheckResponse checkStock(@PathVariable String productId,
                                          @RequestParam int quantity) {
        return inventoryService.checkStock(productId, quantity);
    }
}
