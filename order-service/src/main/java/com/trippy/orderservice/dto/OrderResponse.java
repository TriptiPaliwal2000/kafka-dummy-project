package com.trippy.orderservice.dto;

import com.trippy.orderevents.OrderStatus;
import com.trippy.orderservice.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        String productId,
        int quantity,
        BigDecimal unitPrice,
        String customerEmail,
        OrderStatus status,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
