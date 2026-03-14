package com.ecommerce.ecommerce_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.ecommerce_backend.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /// Customer views their own order history — sorted latest first
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}
