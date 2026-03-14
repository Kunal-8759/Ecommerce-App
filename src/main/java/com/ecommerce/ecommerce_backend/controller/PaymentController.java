package com.ecommerce.ecommerce_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecommerce_backend.dto.request.PaymentRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.PaymentResponseDTO;
import com.ecommerce.ecommerce_backend.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('CUSTOMER')") // Only customers can make payments and view their payment status
@Tag(name = "Payment Management", description = "Payment simulation for placed orders. Successful payment reduces stock and sends confirmation email.")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Customer processes payment for a placed order
    // Simulates success/failure and reduces stock if successful
    @Operation(summary = "Process payment for an order",
               description = """
                       Simulates a payment gateway with 70% success rate.
                       
                       **On SUCCESS:**
                       - `paymentStatus` → SUCCESS
                       - Stock reduced per item
                       - Cart cleared
                       - Order confirmation email sent
                       
                       **On FAILURE:**
                       - `paymentStatus` → FAILED
                       - Stock untouched
                       - Cart stays intact
                       
                       **Rules:**
                       - Cannot pay an already paid order
                       - Cannot pay after the 10-minute payment deadline
                       - Can only pay your own order
                       """)
    @PostMapping("/pay/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> processPayment(@PathVariable Long orderId,@Valid @RequestBody PaymentRequestDTO request) {

        PaymentResponseDTO data = paymentService.processPayment(orderId, request);

        // Determine message based on payment outcome
        boolean isSuccess = data.getPaymentStatus().equals("SUCCESS");

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        isSuccess ? "Payment processed successfully" : "Payment processing failed",
                        data));
    }

    // Customer checks payment status of their order
    @Operation(summary = "Get payment status of an order",
               description = "Returns current payment status, order status, and payment method for an order. Customer can only check their own order.")
    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentStatus(
            @PathVariable Long orderId) {

        PaymentResponseDTO data = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Payment status fetched successfully", data));
    }

}
