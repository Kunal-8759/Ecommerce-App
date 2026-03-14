package com.ecommerce.ecommerce_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecommerce_backend.dto.request.UpdateOrderStatusRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.OrderResponseDTO;
import com.ecommerce.ecommerce_backend.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Checkout, view order history and admin order status updates.")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {
    @Autowired
    private OrderService orderService;

    // Customer places order — converts cart into order
    @Operation(summary = "Checkout — place an order",
               description = """
                       Converts the customer's cart into an order.
                       - Cart must not be empty
                       - Stock is validated before order creation
                       - Payment deadline is set to 10 minutes from checkout
                       - Cart is NOT cleared until payment succeeds
                       """)
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> checkout() {
        OrderResponseDTO data = orderService.checkout();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Order placed successfully", data));
    }

    // Customer views their own order history
    @Operation(summary = "Get my order history",
               description = "Returns all orders placed by the logged-in customer, sorted by latest first.")
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getMyOrders() {
        List<OrderResponseDTO> data = orderService.getMyOrders();
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Orders fetched successfully", data));
    }


    // Get Order By Id
    @Operation(summary = "Get order by ID",
               description = "Customer can only view their own orders. Admin can view any order.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')") // Admin can view any order, customer can view own orders (enforced in service)")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable Long id) {
        OrderResponseDTO data = orderService.getOrderById(id);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Order fetched successfully", data));
    }

    // Admin only — update order status (PLACED → SHIPPED → DELIVERED or CANCELLED)
    @Operation(summary = "Update order status (Admin only)",
               description = "Admin updates the order status. Valid transitions: PLACED → SHIPPED → DELIVERED or CANCELLED. Cannot update a CANCELLED order.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequestDTO request) {
        OrderResponseDTO data = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Order status updated successfully", data));
    }
}
