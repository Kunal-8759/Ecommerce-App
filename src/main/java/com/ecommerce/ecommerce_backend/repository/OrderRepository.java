package com.ecommerce.ecommerce_backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.PaymentStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /// Customer views their own order history — sorted latest first
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    // Used by scheduler: find all PENDING orders past their payment deadline
    // These are orders where customer never paid within the allowed window
    List<Order> findByPaymentStatusAndPaymentDeadlineBefore(PaymentStatus paymentStatus,LocalDateTime deadline);
}
