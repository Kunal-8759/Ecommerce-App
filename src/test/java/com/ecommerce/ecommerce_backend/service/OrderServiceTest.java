package com.ecommerce.ecommerce_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.quality.Strictness;

import com.ecommerce.ecommerce_backend.dto.request.UpdateOrderStatusRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.OrderResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Cart;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.Order;
import com.ecommerce.ecommerce_backend.entity.OrderStatus;
import com.ecommerce.ecommerce_backend.entity.PaymentStatus;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.InsufficientStockException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.OrderRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User mockUser;
    private Product mockProduct;
    private Cart mockCart;
    private CartItem mockCartItem;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setRole(Role.CUSTOMER);

        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Samsung Galaxy S24");
        mockProduct.setPrice(new BigDecimal("79999.00"));
        mockProduct.setStock(10);

        mockCartItem = new CartItem();
        mockCartItem.setId(1L);
        mockCartItem.setProduct(mockProduct);
        mockCartItem.setQuantity(2);

        mockCart = new Cart();
        mockCart.setId(1L);
        mockCart.setUser(mockUser);
        mockCart.setTotalPrice(new BigDecimal("159998.00"));
        mockCart.setCartItems(new ArrayList<>(List.of(mockCartItem)));

        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setUser(mockUser);
        mockOrder.setTotalAmount(new BigDecimal("159998.00"));
        mockOrder.setPaymentStatus(PaymentStatus.PENDING);
        mockOrder.setOrderStatus(OrderStatus.PLACED);
        mockOrder.setOrderItems(new ArrayList<>());

        // Mock SecurityContext
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    //  Checkout 

    @Test
    void checkout_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        OrderResponseDTO result = orderService.checkout();

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void checkout_ThrowsException_WhenCartIsEmpty() {
        mockCart.setCartItems(new ArrayList<>()); // empty cart

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));

        assertThrows(IllegalStateException.class, () -> orderService.checkout());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_ThrowsInsufficientStock_WhenStockNotEnough() {
        mockProduct.setStock(1);   // stock: 1
        mockCartItem.setQuantity(5); // requested: 5

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));

        assertThrows(InsufficientStockException.class, () -> orderService.checkout());
        verify(orderRepository, never()).save(any());
    }

    //  Get Order By ID 

    @Test
    void getOrderById_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderResponseDTO result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
    }

    @Test
    void getOrderById_ThrowsNotFoundException_WhenOrderDoesNotExist() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(99L));
    }

    //  Update Order Status 

    @Test
    void updateOrderStatus_Success() {
        UpdateOrderStatusRequestDTO request = new UpdateOrderStatusRequestDTO();
        request.setOrderStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        OrderResponseDTO result = orderService.updateOrderStatus(1L, request);

        assertNotNull(result);
        verify(orderRepository, times(1)).save(mockOrder);
        assertEquals(OrderStatus.SHIPPED, mockOrder.getOrderStatus());
    }

    @Test
    void updateOrderStatus_ThrowsException_WhenOrderIsCancelled() {
        mockOrder.setOrderStatus(OrderStatus.CANCELLED);
        UpdateOrderStatusRequestDTO request = new UpdateOrderStatusRequestDTO();
        request.setOrderStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(1L, request));

        verify(orderRepository, never()).save(any());
    }
}