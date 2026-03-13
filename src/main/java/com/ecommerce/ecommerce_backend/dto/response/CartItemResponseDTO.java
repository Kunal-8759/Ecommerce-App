package com.ecommerce.ecommerce_backend.dto.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CartItemResponseDTO {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
