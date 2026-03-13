package com.ecommerce.ecommerce_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecommerce_backend.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart,Long> {

    // Find cart by the owner's user ID
    Optional<Cart> findByUserId(Long userId);
}
