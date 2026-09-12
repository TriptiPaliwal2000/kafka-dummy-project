package com.trippy.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trippy.orderevents.OrderStatus;
import com.trippy.orderservice.dto.CreateOrderRequest;
import com.trippy.orderservice.dto.OrderResponse;
import com.trippy.orderservice.exception.OrderNotFoundException;
import com.trippy.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: boots only the MVC machinery (no real JPA/Kafka/Eureka), with
 * OrderService mocked out, to verify routing, request validation and status codes.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrder_withValidRequest_returns201AndBody() throws Exception {
        OrderResponse response = new OrderResponse(1L, "PRODUCT-1", 2, BigDecimal.valueOf(9.99),
                "a@b.com", OrderStatus.CONFIRMED, Instant.now());
        when(orderService.placeOrder(any())).thenReturn(response);

        CreateOrderRequest request = new CreateOrderRequest("PRODUCT-1", 2, "a@b.com");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void placeOrder_withBlankProductId_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("", 2, "a@b.com");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.productId").exists());
    }

    @Test
    void placeOrder_withNonPositiveQuantity_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("PRODUCT-1", 0, "a@b.com");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_whenNotFound_returns404() throws Exception {
        when(orderService.getOrder(eq(99L))).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order 99 was not found"));
    }
}
