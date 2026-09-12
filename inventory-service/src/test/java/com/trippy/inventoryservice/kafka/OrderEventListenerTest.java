package com.trippy.inventoryservice.kafka;

import com.trippy.orderevents.OrderPlacedEvent;
import com.trippy.orderevents.OrderStatus;
import com.trippy.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private InventoryService inventoryService;

    @Test
    void onOrderPlaced_whenConfirmed_decrementsStock() {
        OrderEventListener listener = new OrderEventListener(inventoryService);
        OrderPlacedEvent event = OrderPlacedEvent.of(1L, "PRODUCT-1", 2, BigDecimal.TEN,
                "a@b.com", OrderStatus.CONFIRMED);

        listener.onOrderPlaced(event);

        verify(inventoryService).decrementStock("PRODUCT-1", 2);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    void onOrderPlaced_whenNotConfirmed_neverTouchesStock(OrderStatus status) {
        OrderEventListener listener = new OrderEventListener(inventoryService);
        OrderPlacedEvent event = OrderPlacedEvent.of(2L, "PRODUCT-1", 1, BigDecimal.TEN, "a@b.com", status);

        listener.onOrderPlaced(event);

        verify(inventoryService, never()).decrementStock("PRODUCT-1", 1);
    }
}
