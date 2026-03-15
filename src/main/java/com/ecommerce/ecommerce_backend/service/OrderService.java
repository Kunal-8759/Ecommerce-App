package com.ecommerce.ecommerce_backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ecommerce.ecommerce_backend.dto.request.UpdateOrderStatusRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.OrderItemResponseDTO;
import com.ecommerce.ecommerce_backend.dto.response.OrderResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Cart;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderItem;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.InsufficientStockException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.OrderRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    // get logged in user from security context
    private User getLoggedInUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        log.debug("Resolving logged-in user — email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() ->{
                    log.warn("Logged-in user not found with email: {}", email);
                    return new ResourceNotFoundException("Logged in user not found");
                });
    }

    /*  
        CHECKOUT : Cart → Order
        * 1. Get the user's cart
        * 2. Cart must not be empty
        * 3. Validate stock for ALL items before creating anything
        * 4. Create the Order
        * 5. Convert each CartItem → OrderItem (snapshot price at this moment)
        * 6. Clear the cart after successful order creation
        * 
        * 
        * @Transactional ensures: if anything fails midway,
        * the entire operation rolls back — no partial orders saved
    */
    @Transactional
    public OrderResponseDTO checkout() {
        User user = getLoggedInUser();

        log.info("Checkout initiated — userId: {}", user.getId());

        // 1. Get the user's cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.warn("Cart not found for user — userId: {}", user.getId());
                    return new ResourceNotFoundException("Cart not found. Add items before checkout.");
                });

        // 2. Cart must not be empty
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            log.warn("Cart is empty for user — userId: {}", user.getId());
            throw new IllegalStateException("Cart is empty. Add items before checkout.");
        }

        log.debug("Validating stock for {} cart item(s) — userId: {}",
                cart.getCartItems().size(), user.getId());

        // 3. Validate stock for ALL items before creating anything
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getProduct().getStock() < cartItem.getQuantity()) {
                log.warn("Insufficient stock for product — userId: {}, productId: {}, available: {}, requested: {}",
                        user.getId(), cartItem.getProduct().getId(),
                        cartItem.getProduct().getStock(), cartItem.getQuantity());
                throw new InsufficientStockException("Insufficient stock for '" + cartItem.getProduct().getName()+ "'. Available: " + cartItem.getProduct().getStock() + ", Requested: " + cartItem.getQuantity());
            }
        }

        // 4. Create the Order
        // @PrePersist on Order auto-sets: orderDate, paymentStatus=PENDING, orderStatus=PLACED
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(cart.getTotalPrice());

        // 5. Convert each CartItem → OrderItem (snapshot price at this moment)
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        log.info("Order placed successfully — orderId: {}, userId: {}, " +
                "items: {}, totalAmount: {}, paymentDeadline: {}",
                savedOrder.getId(),
                user.getId(),
                savedOrder.getOrderItems().size(),
                savedOrder.getTotalAmount(),
                savedOrder.getPaymentDeadline());

        return mapToOrderResponseDTO(savedOrder); 
    }


    // CUSTOMER : Get My Order History 
    public List<OrderResponseDTO> getMyOrders() {
        User user = getLoggedInUser();
        log.debug("Fetching order history for userId: {}", user.getId());

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());

        log.debug("Order history fetched — userId: {}, totalOrders: {}",
                user.getId(), orders.size());

        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    // CUSTOMER : Get Single Order by ID
    public OrderResponseDTO getOrderById(Long orderId) {
        User user = getLoggedInUser();

        log.debug("Fetching order — orderId: {}, requestedBy userId: {}",
                orderId, user.getId());

        Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException( "Order not found with id: " + orderId));

        // If customer, ensure they own this order
        if (user.getRole().name().equals("CUSTOMER") && !order.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized order access attempt — " +
                    "requestingUserId: {}, orderOwnerUserId: {}, orderId: {}",
                    user.getId(), order.getUser().getId(), orderId);
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return mapToOrderResponseDTO(order);
    }

    //  ADMIN : Update Order Status
    public OrderResponseDTO updateOrderStatus(Long orderId,UpdateOrderStatusRequestDTO request) {
        log.info("Admin updating order status — orderId: {}, requestedStatus: {}",
                orderId, request.getOrderStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found for status update — orderId: {}", orderId);
                    return new ResourceNotFoundException("Order not found with id: " + orderId);
                });

        // Business rule: cannot change status of a CANCELLED order
        if (order.getOrderStatus().name().equals("CANCELLED")) {
            log.warn("Status update blocked — order is already CANCELLED. orderId: {}", orderId);
            throw new IllegalStateException("Cannot update status of a cancelled order");
        }

        order.setOrderStatus(request.getOrderStatus());
        Order updated = orderRepository.save(order);

        log.info("Order status updated successfully — orderId: {}, newStatus: {}",
                orderId, request.getOrderStatus());

        return mapToOrderResponseDTO(updated);
    }

    //  Map Order → OrderResponseDTO 
    private OrderResponseDTO mapToOrderResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setCustomerName(order.getUser().getName());
        dto.setCustomerEmail(order.getUser().getEmail());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderDate(order.getOrderDate());
        dto.setPaymentDeadline(order.getPaymentDeadline());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null );
        dto.setOrderStatus(order.getOrderStatus().name());

        List<OrderItemResponseDTO> itemDTOs = order.getOrderItems()
                .stream()
                .map(this::mapToOrderItemResponseDTO)
                .collect(Collectors.toList());

        dto.setOrderItems(itemDTOs);
        return dto;
    }

    //  Map OrderItem → OrderItemResponseDTO 
    private OrderItemResponseDTO mapToOrderItemResponseDTO(OrderItem item) {
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setOrderItemId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice()); // snapshotted price
        dto.setSubtotal(
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }
}
