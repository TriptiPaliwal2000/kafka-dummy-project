package com.trippy.inventoryservice.controller;

import com.trippy.inventoryservice.dto.InventoryItemResponse;
import com.trippy.inventoryservice.dto.StockCheckResponse;
import com.trippy.inventoryservice.exception.ProductNotFoundException;
import com.trippy.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void getProduct_returnsInventoryDetails() throws Exception {
        when(inventoryService.getProduct("PRODUCT-1"))
                .thenReturn(new InventoryItemResponse("PRODUCT-1", "Keyboard", 10, BigDecimal.valueOf(89.99)));

        mockMvc.perform(get("/api/inventory/PRODUCT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Keyboard"))
                .andExpect(jsonPath("$.availableStock").value(10));
    }

    @Test
    void getProduct_whenMissing_returns404() throws Exception {
        when(inventoryService.getProduct("GHOST")).thenThrow(new ProductNotFoundException("GHOST"));

        mockMvc.perform(get("/api/inventory/GHOST"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkStock_returnsAvailability() throws Exception {
        when(inventoryService.checkStock("PRODUCT-1", 3))
                .thenReturn(new StockCheckResponse(StockCheckResponse.Availability.AVAILABLE, 10, BigDecimal.valueOf(89.99)));

        mockMvc.perform(get("/api/inventory/PRODUCT-1/check").param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("AVAILABLE"));
    }
}
