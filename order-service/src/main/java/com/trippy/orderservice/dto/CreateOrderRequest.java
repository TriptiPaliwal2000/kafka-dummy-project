package com.trippy.orderservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(

        @NotBlank(message = "productId is required")
        String productId,

        @Positive(message = "quantity must be greater than 0")
        int quantity,

        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail must be a valid email address")
        String customerEmail
) {
}
