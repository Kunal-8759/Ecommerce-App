package com.ecommerce.ecommerce_backend.dto.request;

import com.ecommerce.ecommerce_backend.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequestDTO {
    @NotNull(message = "Order status is required")
    private OrderStatus orderStatus;
}
