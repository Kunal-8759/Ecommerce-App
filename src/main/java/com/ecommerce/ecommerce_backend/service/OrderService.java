package com.ecommerce.ecommerce_backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found"));
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

        // 1. Get the user's cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found. Add items before checkout."));

        // 2. Cart must not be empty
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty. Add items before checkout.");
        }

        // 3. Validate stock for ALL items before creating anything
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getProduct().getStock() < cartItem.getQuantity()) {
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

        // 6. Clear the cart after successful order creation
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);

        return mapToOrderResponseDTO(savedOrder); 
    }


    // CUSTOMER : Get My Order History 
    public List<OrderResponseDTO> getMyOrders() {
        User user = getLoggedInUser();
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());

        return orders.stream()
                .map(this::mapToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    // CUSTOMER : Get Single Order by ID
    public OrderResponseDTO getOrderById(Long orderId) {
        User user = getLoggedInUser();

        Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException( "Order not found with id: " + orderId));

        // If customer, ensure they own this order
        if (user.getRole().name().equals("CUSTOMER") && !order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return mapToOrderResponseDTO(order);
    }

    //  ADMIN : Update Order Status
    public OrderResponseDTO updateOrderStatus(Long orderId,UpdateOrderStatusRequestDTO request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Business rule: cannot change status of a CANCELLED order
        if (order.getOrderStatus().name().equals("CANCELLED")) {
            throw new IllegalStateException("Cannot update status of a cancelled order");
        }

        order.setOrderStatus(request.getOrderStatus());
        Order updated = orderRepository.save(order);
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
