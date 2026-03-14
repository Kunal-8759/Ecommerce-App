package com.ecommerce.ecommerce_backend.utils;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderStatus;
import com.ecommerce.ecommerce_backend.entity.PaymentStatus;
import com.ecommerce.ecommerce_backend.repository.OrderRepository;

import jakarta.transaction.Transactional;

@Component
public class OrderExpiryScheduler {
    @Autowired
    private OrderRepository orderRepository;

    // Runs every 60 seconds
    // Finds all PENDING orders whose paymentDeadline has passed
    // Cancels them — frees up the "spoken for" slot in business logic
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderRepository.findByPaymentStatusAndPaymentDeadlineBefore(PaymentStatus.PENDING,LocalDateTime.now());

        if (expiredOrders.isEmpty()) {
            return; // Nothing to process
        }

        for (Order order : expiredOrders) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            System.out.println(
                    "[OrderExpiryScheduler] Auto-cancelled expired order ID: "
                    + order.getId()
                    + " | Customer: " + order.getUser().getEmail()
                    + " | Deadline was: " + order.getPaymentDeadline());
        }

        System.out.println(
                "[OrderExpiryScheduler] Processed " + expiredOrders.size()
                + " expired order(s) at " + LocalDateTime.now());
    }
}
