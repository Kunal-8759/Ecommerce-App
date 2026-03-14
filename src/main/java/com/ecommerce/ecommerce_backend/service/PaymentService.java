package com.ecommerce.ecommerce_backend.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ecommerce.ecommerce_backend.dto.request.PaymentRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.PaymentResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderItem;
import com.ecommerce.ecommerce_backend.entity.OrderStatus;
import com.ecommerce.ecommerce_backend.entity.PaymentStatus;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.InsufficientStockException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.exception.UnauthorizedAccessException;
import com.ecommerce.ecommerce_backend.repository.OrderRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    //  Get logged-in User from SecurityContext 
    private User getLoggedInUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found"));
    }



    /*
        * 1. Validate order exists and belongs to logged-in user
        * 2. Prevent re-payment — only PENDING orders can be paid
        * 3. Validate stock for ALL items again just before payment
        *    (stock could have changed since checkout)
        * 4. Simulate payment gateway response (success/failure)
        * 5. If success → update order payment status, reduce stock for each product
        * 6. If failure → update order payment status, do NOT reduce stock
        * 7. Return appropriate response to frontend for user feedback
        *
    */
    @Transactional
    public PaymentResponseDTO processPayment(Long orderId, PaymentRequestDTO request) {

        User user = getLoggedInUser();

        // 1. Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // 2. Security check — customer can only pay for their own order
        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to process payment for this order");
        }

        // 3. Prevent re-payment — only PENDING orders can be paid
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException(
                    "Payment already completed for order id: " + orderId);
        }

        if (order.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new IllegalStateException(
                    "This order's payment has failed. Please place a new order.");
        }

        // 4. Check if payment deadline has passed
        if (LocalDateTime.now().isAfter(order.getPaymentDeadline())) {
            // Mark as CANCELLED since deadline passed
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new IllegalStateException(
                    "Payment deadline has passed for order id: " + orderId + ". Please place a new order.");
        }

        // 4. Validate stock again just before payment
        // Stock could have changed between checkout and payment time
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            if (product.getStock() < orderItem.getQuantity()) {
                // Mark payment as failed and save — stock ran out
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setPaymentMethod(request.getPaymentMethod());
                orderRepository.save(order);

                throw new InsufficientStockException("Payment failed. Insufficient stock for '"+ product.getName() + "'. Available: "+ product.getStock());
            }
        }

        // 5. Store the payment method on the order
        order.setPaymentMethod(request.getPaymentMethod());

        // 6. Simulate payment
        boolean paymentSuccessful = simulatePaymentGateway();

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setOrderId(orderId);
        response.setPaymentMethod(request.getPaymentMethod().name());
        response.setAmountPaid(order.getTotalAmount());
        response.setProcessedAt(LocalDateTime.now());

        if (paymentSuccessful) {
            // Update payment status on order
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            orderRepository.save(order);

            // Reduce stock for every item in the order — Inventory Management
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                int newStock = product.getStock() - orderItem.getQuantity();
                product.setStock(newStock);
                productRepository.save(product);
            }

            response.setPaymentStatus(PaymentStatus.SUCCESS.name());
            response.setOrderStatus(order.getOrderStatus().name());
            response.setMessage("Payment successful! Your order has been confirmed. " +"Order ID: " + orderId);

        } else {//Payment Failure
            // Stock is NOT reduced — order stays PLACED but payment FAILED
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            response.setPaymentStatus(PaymentStatus.FAILED.name());
            response.setOrderStatus(order.getOrderStatus().name());
            response.setMessage("Payment failed. Please try again or use a different payment method.");
        }

        return response;
    }

    //  GET PAYMENT STATUS of an Order 
    public PaymentResponseDTO getPaymentStatus(Long orderId) {
        User user = getLoggedInUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Customer can only check their own order's payment
        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view payment for this order");
        }

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setOrderId(orderId);
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setOrderStatus(order.getOrderStatus().name());
        response.setPaymentMethod(
                order.getPaymentMethod() != null
                        ? order.getPaymentMethod().name()
                        : "NOT_PROCESSED_YET");
        response.setAmountPaid(order.getTotalAmount());
        response.setMessage(buildStatusMessage(order.getPaymentStatus()));
        response.setProcessedAt(order.getOrderDate());

        return response;
    }

    //  Simulate Payment Gateway 
    // 70% chance of success — realistic for a payment simulation
    // In production this would be replaced with Razorpay/Stripe/PayU  call
    private boolean simulatePaymentGateway() {
        return new Random().nextInt(100) < 95;
    }

    //  Build human-readable status message 
    private String buildStatusMessage(PaymentStatus status) {
        switch (status) {
            case PENDING:
                return "Payment is pending. Please complete the payment.";
            case SUCCESS:
                return "Payment completed successfully.";
            case FAILED:
                return "Payment failed. Please place a new order.";
            default:
                return "Unknown payment status.";
        }
    }
}
