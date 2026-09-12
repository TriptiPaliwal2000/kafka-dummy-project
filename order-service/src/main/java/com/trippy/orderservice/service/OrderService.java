package com.trippy.orderservice.service;

import com.trippy.orderservice.dto.CreateOrderRequest;
import com.trippy.orderservice.dto.OrderResponse;

import java.util.List;

/**
 * Single Responsibility: orchestrates the order lifecycle only (validate -> check stock ->
 * persist -> publish event). It does not know how stock is checked (that's InventoryClient)
 * or how events are transported (that's OrderEventPublisher) - see their Javadoc.
 */
public interface OrderService {

    OrderResponse placeOrder(CreateOrderRequest request);

    OrderResponse getOrder(Long orderId);

    List<OrderResponse> listOrders();
}
