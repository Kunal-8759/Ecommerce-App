package com.ecommerce.ecommerce_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDTO {
    // Customer tells us which payment method they're using
    // In simulation, this doesn't affect logic but stored for order record realism
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        UPI,
        NET_BANKING,
    }
}
