package com.ecommerce.ecommerce_backend.utils;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(OrderExpiryScheduler.class);

    @Autowired
    private OrderRepository orderRepository;

    // Runs every 60 seconds
    // Finds all PENDING orders whose paymentDeadline has passed
    // Cancels them — frees up the "spoken for" slot in business logic
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {

        log.debug("[OrderExpiryScheduler] Running order expiry check at {}", LocalDateTime.now());

        List<Order> expiredOrders = orderRepository.findByPaymentStatusAndPaymentDeadlineBefore(PaymentStatus.PENDING,LocalDateTime.now());

        if (expiredOrders.isEmpty()) {
            log.debug("[OrderExpiryScheduler] No expired orders found at {}", LocalDateTime.now());
            return; // Nothing to process
        }

        for (Order order : expiredOrders) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.warn("Order auto-cancelled — payment deadline exceeded. " +
                     "orderId: {}, userId: {}, customerEmail: {}, deadlineWas: {}",
                    order.getId(),
                    order.getUser().getId(),
                    order.getUser().getEmail(),
                    order.getPaymentDeadline());
        }

        log.info("[OrderExpiryScheduler] Processed " + expiredOrders.size()
                + " expired order(s) at " + LocalDateTime.now());
    }
}
