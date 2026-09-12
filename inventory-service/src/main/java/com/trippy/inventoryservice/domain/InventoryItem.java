package com.trippy.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * inventory-service's view of a product: just enough to answer "is there stock" and to keep
 * it accurate under concurrent updates. {@code @Version} gives us optimistic locking so two
 * near-simultaneous decrements (e.g. a Kafka listener thread and a concurrent REST admin
 * update) can't silently overwrite each other.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "available_stock", nullable = false)
    private int availableStock;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Version
    private long version;

    protected InventoryItem() {
        // JPA
    }

    public InventoryItem(String productId, String productName, int availableStock, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.availableStock = availableStock;
        this.unitPrice = unitPrice;
    }

    public boolean hasStockFor(int quantity) {
        return availableStock >= quantity;
    }

    public void decrementStock(int quantity) {
        if (quantity > availableStock) {
            throw new IllegalStateException(
                    "Cannot decrement %d units of %s - only %d in stock".formatted(quantity, productId, availableStock));
        }
        this.availableStock -= quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public long getVersion() {
        return version;
    }
}
