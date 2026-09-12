package com.trippy.inventoryservice.kafka;

import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.orderevents.OrderStatus;
import com.trippy.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order-service's OrderPlacedEvent and keeps stock levels in sync. Only CONFIRMED
 * orders actually move stock - REJECTED_OUT_OF_STOCK orders never had stock reserved, and
 * PENDING_VERIFICATION orders are, by definition, orders inventory-service couldn't confirm
 * synchronously, so decrementing here would double-count if/when they're later reconciled.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final InventoryService inventoryService;

    public OrderEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "${app.kafka.order-events-topic:order-events}", groupId = "${spring.kafka.consumer.group-id:inventory-service}")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent order={} product={} qty={} status={}",
                event.orderId(), event.productId(), event.quantity(), event.status());

        if (event.status() == OrderStatus.CONFIRMED) {
            inventoryService.decrementStock(event.productId(), event.quantity());
        } else {
            log.info("Order {} has status {} - no stock movement needed", event.orderId(), event.status());
        }
    }
}
