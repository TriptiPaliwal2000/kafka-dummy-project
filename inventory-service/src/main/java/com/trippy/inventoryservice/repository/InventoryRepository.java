package com.trippy.inventoryservice.repository;

import com.trippy.inventoryservice.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
}
