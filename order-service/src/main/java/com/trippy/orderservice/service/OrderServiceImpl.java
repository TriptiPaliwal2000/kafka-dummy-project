package com.trippy.orderservice.service;

import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.orderevents.OrderStatus;
import com.trippy.orderservice.client.InventoryClient;
import com.trippy.orderservice.client.StockCheckResult;
import com.trippy.orderservice.domain.Order;
import com.trippy.orderservice.dto.CreateOrderRequest;
import com.trippy.orderservice.dto.OrderResponse;
import com.trippy.orderservice.event.OrderEventPublisher;
import com.trippy.orderservice.exception.OrderNotFoundException;
import com.trippy.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                             InventoryClient inventoryClient,
                             OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderResponse placeOrder(CreateOrderRequest request) {
        StockCheckResult stockCheck = checkStockSafely(request.productId(), request.quantity());

        OrderStatus status;
        BigDecimal unitPrice;
        switch (stockCheck.availability()) {
            case AVAILABLE -> {
                status = OrderStatus.CONFIRMED;
                unitPrice = stockCheck.unitPrice();
            }
            case UNAVAILABLE -> {
                status = OrderStatus.REJECTED_OUT_OF_STOCK;
                unitPrice = stockCheck.unitPrice();
            }
            default -> {
                // inventory-service was unreachable / circuit open / timed out.
                status = OrderStatus.PENDING_VERIFICATION;
                unitPrice = null;
            }
        }

        Order order = new Order(request.productId(), request.quantity(), unitPrice,
                request.customerEmail(), status);
        order = orderRepository.save(order);

        eventPublisher.publish(OrderPlacedEvent.of(order.getId(), order.getProductId(), order.getQuantity(),
                unitPrice, order.getCustomerEmail(), status));

        return OrderResponse.from(order);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public List<OrderResponse> listOrders() {
        return orderRepository.findAll().stream().map(OrderResponse::from).toList();
    }

    private StockCheckResult checkStockSafely(String productId, int quantity) {
        try {
            // The circuit breaker / time limiter / fallback all live inside InventoryClient;
            // from here this is just "give me a result, bounded in time".
            return inventoryClient.checkStock(productId, quantity).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while checking stock for {}", productId, e);
            return StockCheckResult.unknown();
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Stock check did not complete for {}: {}", productId, e.getMessage());
            return StockCheckResult.unknown();
        }
    }
}
