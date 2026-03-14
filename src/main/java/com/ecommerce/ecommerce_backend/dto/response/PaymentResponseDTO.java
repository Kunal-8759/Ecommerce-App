package com.ecommerce.ecommerce_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PaymentResponseDTO {
    private Long orderId;
    private String paymentStatus;   
    private String orderStatus;    
    private String paymentMethod;
    private BigDecimal amountPaid;
    private String message;         
    private LocalDateTime processedAt;
}
