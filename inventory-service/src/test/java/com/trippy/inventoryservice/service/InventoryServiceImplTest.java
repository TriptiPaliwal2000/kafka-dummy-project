package com.trippy.inventoryservice.service;

import com.trippy.inventoryservice.domain.InventoryItem;
import com.trippy.inventoryservice.dto.InventoryItemResponse;
import com.trippy.inventoryservice.dto.StockCheckResponse;
import com.trippy.inventoryservice.exception.ProductNotFoundException;
import com.trippy.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(inventoryRepository);
    }

    @Test
    void checkStock_whenEnoughStock_returnsAvailable() {
        InventoryItem item = new InventoryItem("PRODUCT-1", "Keyboard", 10, BigDecimal.valueOf(89.99));
        when(inventoryRepository.findById("PRODUCT-1")).thenReturn(Optional.of(item));

        StockCheckResponse response = inventoryService.checkStock("PRODUCT-1", 3);

        assertThat(response.availability()).isEqualTo(StockCheckResponse.Availability.AVAILABLE);
        assertThat(response.currentStock()).isEqualTo(10);
    }

    @Test
    void checkStock_whenNotEnoughStock_returnsUnavailable() {
        InventoryItem item = new InventoryItem("PRODUCT-1", "Keyboard", 2, BigDecimal.valueOf(89.99));
        when(inventoryRepository.findById("PRODUCT-1")).thenReturn(Optional.of(item));

        StockCheckResponse response = inventoryService.checkStock("PRODUCT-1", 5);

        assertThat(response.availability()).isEqualTo(StockCheckResponse.Availability.UNAVAILABLE);
    }

    @Test
    void checkStock_whenProductUnknown_throws() {
        when(inventoryRepository.findById("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.checkStock("GHOST", 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void decrementStock_whenEnoughStock_savesReducedQuantity() {
        InventoryItem item = new InventoryItem("PRODUCT-1", "Keyboard", 10, BigDecimal.valueOf(89.99));
        when(inventoryRepository.findById("PRODUCT-1")).thenReturn(Optional.of(item));

        inventoryService.decrementStock("PRODUCT-1", 4);

        assertThat(item.getAvailableStock()).isEqualTo(6);
        verify(inventoryRepository).save(item);
    }

    @Test
    void decrementStock_whenNotEnoughStock_doesNotSaveOrThrow() {
        InventoryItem item = new InventoryItem("PRODUCT-1", "Keyboard", 1, BigDecimal.valueOf(89.99));
        when(inventoryRepository.findById("PRODUCT-1")).thenReturn(Optional.of(item));

        // A stock mismatch at this point means order-service already confirmed the order -
        // we log it for reconciliation rather than throwing and losing the event.
        inventoryService.decrementStock("PRODUCT-1", 5);

        assertThat(item.getAvailableStock()).isEqualTo(1);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void getProduct_returnsMappedResponse() {
        InventoryItem item = new InventoryItem("PRODUCT-9", "Webcam", 7, BigDecimal.valueOf(49.99));
        when(inventoryRepository.findById("PRODUCT-9")).thenReturn(Optional.of(item));

        InventoryItemResponse response = inventoryService.getProduct("PRODUCT-9");

        assertThat(response.productName()).isEqualTo("Webcam");
        assertThat(response.availableStock()).isEqualTo(7);
    }
}
