package com.ecommerce.ecommerce_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecommerce_backend.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Find a specific product inside a specific cart
    // Used to check: "does this product already exist in the cart?"
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
