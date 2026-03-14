package com.ecommerce.ecommerce_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderResponseDTO {
    private Long orderId;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private List<OrderItemResponseDTO> orderItems;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private String paymentStatus;
    private String paymentMethod;
    private String orderStatus;

}
