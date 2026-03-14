package com.ecommerce.ecommerce_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecommerce_backend.dto.request.AddToCartRequestDTO;
import com.ecommerce.ecommerce_backend.dto.request.UpdateCartItemRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.CartResponseDTO;
import com.ecommerce.ecommerce_backend.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')") // entire cart is customer-only
@Tag(name = "Cart Management", description = "Customer cart operations — add, update, remove items and view total. Requires CUSTOMER role.")
@SecurityRequirement(name = "BearerAuth")
public class CartController {

    @Autowired
    private CartService cartService;

    // Add a product to cart (or increase qty if already exists)
    @Operation(summary = "Add product to cart",
               description = "Adds a product to the cart. If the product already exists in the cart, quantity is increased.")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addToCart(@Valid @RequestBody AddToCartRequestDTO request) {
        CartResponseDTO response = cartService.addToCart(request);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Product added to cart successfully", response));
    }

    // Update quantity of a specific product in cart
    @Operation(summary = "Update cart item quantity",
               description = "Updates the quantity of a specific product in the cart.")
    @PutMapping("/update/{productId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateCartItem(@PathVariable Long productId,@Valid @RequestBody UpdateCartItemRequestDTO request) {
        CartResponseDTO response = cartService.updateCartItem(productId, request);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Cart item updated successfully", response));
    }

    // Remove a specific product from cart
    @Operation(summary = "Remove product from cart")
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeFromCart(@PathVariable Long productId) {
        CartResponseDTO response = cartService.removeFromCart(productId);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Product removed from cart successfully", response));
    }

    // View logged-in customer's cart with all items and total
    @Operation(summary = "View my cart",
               description = "Returns the logged-in customer's cart with all items, subtotals, and total price.")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDTO>> getMyCart() {
        CartResponseDTO response = cartService.getMyCart();
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Cart fetched successfully", response));
    }
}
