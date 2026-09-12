package com.trippy.inventoryservice.service;

import com.trippy.inventoryservice.domain.InventoryItem;
import com.trippy.inventoryservice.dto.InventoryItemResponse;
import com.trippy.inventoryservice.dto.StockCheckResponse;
import com.trippy.inventoryservice.exception.ProductNotFoundException;
import com.trippy.inventoryservice.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public InventoryItemResponse getProduct(String productId) {
        return InventoryItemResponse.from(findOrThrow(productId));
    }

    @Override
    public StockCheckResponse checkStock(String productId, int quantity) {
        InventoryItem item = findOrThrow(productId);
        return StockCheckResponse.forItem(item, quantity);
    }

    @Override
    @Transactional
    public void decrementStock(String productId, int quantity) {
        InventoryItem item = findOrThrow(productId);
        if (!item.hasStockFor(quantity)) {
            // This is a reconciliation gap worth logging loudly: order-service confirmed the
            // order based on an earlier stock check, but stock has since moved (e.g. two
            // orders for the last units landed close together). In a production system this
            // would raise an alert / compensating event instead of just logging.
            log.error("Stock mismatch for {}: asked to decrement {} but only {} available",
                    productId, quantity, item.getAvailableStock());
            return;
        }
        item.decrementStock(quantity);
        inventoryRepository.save(item);
        log.info("Decremented stock for {} by {} (remaining: {})", productId, quantity, item.getAvailableStock());
    }

    private InventoryItem findOrThrow(String productId) {
        return inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
