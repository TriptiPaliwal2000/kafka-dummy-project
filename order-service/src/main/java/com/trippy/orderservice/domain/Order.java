package com.trippy.orderservice.domain;

import com.trippy.orderevents.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The order-service write model. Deliberately small: order-service owns only what it needs
 * to answer "what did the customer order and what happened to it" - it does not own product
 * catalog or stock data (that's inventory-service's responsibility).
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    /** Null when the order was accepted in a degraded state and price couldn't be confirmed. */
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Order() {
        // JPA
    }

    public Order(String productId, int quantity, BigDecimal unitPrice, String customerEmail, OrderStatus status) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.customerEmail = customerEmail;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
