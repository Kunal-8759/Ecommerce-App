package com.ecommerce.ecommerce_backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CartResponseDTO {

    private Long cartId;
    private Long userId;
    private List<CartItemResponseDTO> cartItems;
    private BigDecimal totalPrice;
}
