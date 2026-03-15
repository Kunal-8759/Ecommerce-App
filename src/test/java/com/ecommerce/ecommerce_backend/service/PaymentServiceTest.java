package com.ecommerce.ecommerce_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ecommerce.ecommerce_backend.dto.request.PaymentRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.PaymentResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderItem;
import com.ecommerce.ecommerce_backend.entity.OrderStatus;
import com.ecommerce.ecommerce_backend.entity.PaymentStatus;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.UnauthorizedAccessException;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.OrderRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private User mockUser;
    private Product mockProduct;
    private Order mockOrder;
    private OrderItem mockOrderItem;
    private PaymentRequestDTO paymentRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("john@example.com");
        mockUser.setRole(Role.CUSTOMER);

        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Samsung Galaxy S24");
        mockProduct.setPrice(new BigDecimal("79999.00"));
        mockProduct.setStock(10);

        mockOrderItem = new OrderItem();
        mockOrderItem.setId(1L);
        mockOrderItem.setProduct(mockProduct);
        mockOrderItem.setQuantity(2);
        mockOrderItem.setPrice(new BigDecimal("79999.00"));

        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setUser(mockUser);
        mockOrder.setTotalAmount(new BigDecimal("159998.00"));
        mockOrder.setPaymentStatus(PaymentStatus.PENDING);
        mockOrder.setOrderStatus(OrderStatus.PLACED);
        mockOrder.setOrderItems(new ArrayList<>(List.of(mockOrderItem)));
        mockOrder.setPaymentDeadline(LocalDateTime.now().plusMinutes(10));

        paymentRequest = new PaymentRequestDTO();
        paymentRequest.setPaymentMethod(com.ecommerce.ecommerce_backend.dto.request.PaymentRequestDTO.PaymentMethod.UPI);

        // Mock SecurityContext
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    //  Process Payment 

    @Test
    void processPayment_ThrowsUnauthorized_WhenNotOrderOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        mockOrder.setUser(otherUser); // order belongs to different user

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(UnauthorizedAccessException.class,
                () -> paymentService.processPayment(1L, paymentRequest));
    }

    @Test
    void processPayment_ThrowsException_WhenAlreadyPaid() {
        mockOrder.setPaymentStatus(PaymentStatus.SUCCESS); // already paid

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(IllegalStateException.class,
                () -> paymentService.processPayment(1L, paymentRequest));
    }

    @Test
    void processPayment_ThrowsException_WhenDeadlinePassed() {
        mockOrder.setPaymentDeadline(LocalDateTime.now().minusMinutes(1)); // expired

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(IllegalStateException.class,
                () -> paymentService.processPayment(1L, paymentRequest));
    }

    @Test
    void processPayment_ThrowsNotFoundException_WhenOrderDoesNotExist() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> paymentService.processPayment(99L, paymentRequest));
    }

    //  Get Payment Status 

    @Test
    void getPaymentStatus_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        PaymentResponseDTO result = paymentService.getPaymentStatus(1L);

        assertNotNull(result);
        assertEquals("PENDING", result.getPaymentStatus());
        assertEquals(1L, result.getOrderId());
    }

    @Test
    void getPaymentStatus_ThrowsUnauthorized_WhenNotOrderOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        mockOrder.setUser(otherUser);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(UnauthorizedAccessException.class,
                () -> paymentService.getPaymentStatus(1L));
    }
}