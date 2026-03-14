package com.ecommerce.ecommerce_backend.dto.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OrderItemResponseDTO {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;

    // Price paid at the time of order — snapshotted value
    private BigDecimal price;

    // subtotal = price × quantity
    private BigDecimal subtotal;
}
