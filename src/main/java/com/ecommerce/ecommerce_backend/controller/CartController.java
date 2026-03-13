package com.ecommerce.ecommerce_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.ecommerce.ecommerce_backend.dto.response.CartResponseDTO;
import com.ecommerce.ecommerce_backend.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')") // entire cart is customer-only
public class CartController {

    @Autowired
    private CartService cartService;

    // Add a product to cart (or increase qty if already exists)
    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> addToCart(@Valid @RequestBody AddToCartRequestDTO request) {
        CartResponseDTO response = cartService.addToCart(request);
        return ResponseEntity.ok(response);
    }

    // Update quantity of a specific product in cart
    @PutMapping("/update/{productId}")
    public ResponseEntity<CartResponseDTO> updateCartItem(@PathVariable Long productId,@Valid @RequestBody UpdateCartItemRequestDTO request) {
        CartResponseDTO response = cartService.updateCartItem(productId, request);
        return ResponseEntity.ok(response);
    }

    // Remove a specific product from cart
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<CartResponseDTO> removeFromCart(@PathVariable Long productId) {
        CartResponseDTO response = cartService.removeFromCart(productId);
        return ResponseEntity.ok(response);
    }

    // View logged-in customer's cart with all items and total
    @GetMapping
    public ResponseEntity<CartResponseDTO> getMyCart() {
        CartResponseDTO response = cartService.getMyCart();
        return ResponseEntity.ok(response);
    }
}
