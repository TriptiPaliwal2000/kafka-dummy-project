package com.trippy.orderservice.service;

import com.trippy.orderevents.OrderStatus;
import com.trippy.orderservice.client.InventoryClient;
import com.trippy.orderservice.client.StockCheckResult;
import com.trippy.orderservice.domain.Order;
import com.trippy.orderservice.dto.CreateOrderRequest;
import com.trippy.orderservice.dto.OrderResponse;
import com.trippy.orderservice.event.OrderEventPublisher;
import com.trippy.orderservice.exception.OrderNotFoundException;
import com.trippy.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the order-placement workflow: OrderRepository, InventoryClient and
 * OrderEventPublisher are all mocked, so these tests exercise only OrderServiceImpl's own
 * decision logic (what status to assign given a stock-check outcome) - exactly the kind of
 * "mock the repository/service layer" test the Testing gap calls for.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private OrderEventPublisher eventPublisher;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, inventoryClient, eventPublisher);
    }

    @Test
    void placeOrder_whenStockAvailable_confirmsOrderAndPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest("PRODUCT-1", 2, "customer@example.com");
        StockCheckResult available = new StockCheckResult(StockCheckResult.Availability.AVAILABLE, 10, BigDecimal.valueOf(19.99));
        when(inventoryClient.checkStock("PRODUCT-1", 2)).thenReturn(CompletableFuture.completedFuture(available));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.unitPrice()).isEqualTo(BigDecimal.valueOf(19.99));

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(eventPublisher).publish(any());
    }

    @Test
    void placeOrder_whenStockUnavailable_rejectsOrderButStillPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest("PRODUCT-1", 50, "customer@example.com");
        StockCheckResult unavailable = new StockCheckResult(StockCheckResult.Availability.UNAVAILABLE, 3, BigDecimal.valueOf(19.99));
        when(inventoryClient.checkStock("PRODUCT-1", 50)).thenReturn(CompletableFuture.completedFuture(unavailable));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.REJECTED_OUT_OF_STOCK);
        // Notification-service still needs to hear about rejections so it can tell the customer.
        verify(eventPublisher).publish(any());
    }

    @Test
    void placeOrder_whenInventoryServiceUnreachable_degradesToPendingVerification() {
        CreateOrderRequest request = new CreateOrderRequest("PRODUCT-1", 1, "customer@example.com");
        // Simulates what InventoryClient.checkStockFallback returns when the circuit is open.
        when(inventoryClient.checkStock("PRODUCT-1", 1))
                .thenReturn(CompletableFuture.completedFuture(StockCheckResult.unknown()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_VERIFICATION);
        assertThat(response.unitPrice()).isNull();
        verify(eventPublisher).publish(any());
    }

    @Test
    void getOrder_whenOrderDoesNotExist_throwsNotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(404L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getOrder_whenOrderExists_returnsMappedResponse() {
        Order order = new Order("PRODUCT-2", 3, BigDecimal.TEN, "a@b.com", OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(1L);

        assertThat(response.productId()).isEqualTo("PRODUCT-2");
        assertThat(response.quantity()).isEqualTo(3);
    }
}
