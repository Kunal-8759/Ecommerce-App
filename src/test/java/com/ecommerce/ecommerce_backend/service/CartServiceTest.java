package com.ecommerce.ecommerce_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
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

import com.ecommerce.ecommerce_backend.dto.request.AddToCartRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.CartResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Cart;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.InsufficientStockException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.CartItemRepository;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User mockUser;
    private Product mockProduct;
    private Cart mockCart;

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

        mockCart = new Cart();
        mockCart.setId(1L);
        mockCart.setUser(mockUser);
        mockCart.setCartItems(new ArrayList<>());
        mockCart.setTotalPrice(BigDecimal.ZERO);

        // Mock SecurityContext so CartService can get the logged-in user
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    //  Add To Cart 

    @Test
    void addToCart_Success_NewItem() {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(1L);
        request.setQuantity(2);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(new CartItem());
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        CartResponseDTO result = cartService.addToCart(request);

        assertNotNull(result);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    void addToCart_ThrowsInsufficientStock_WhenQuantityExceedsStock() {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(1L);
        request.setQuantity(99); // stock is only 10

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        assertThrows(InsufficientStockException.class,
                () -> cartService.addToCart(request));

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addToCart_ThrowsNotFoundException_WhenProductDoesNotExist() {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(99L);
        request.setQuantity(1);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.addToCart(request));
    }

    //  Remove From Cart 

    @Test
    void removeFromCart_Success() {
        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setProduct(mockProduct);
        cartItem.setQuantity(2);
        cartItem.setCart(mockCart);
        mockCart.getCartItems().add(cartItem);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(cartItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        CartResponseDTO result = cartService.removeFromCart(1L);

        assertNotNull(result);
        verify(cartItemRepository, times(1)).delete(cartItem);
    }

    @Test
    void removeFromCart_ThrowsNotFoundException_WhenProductNotInCart() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.removeFromCart(99L));
    }

    //  View Cart 

    @Test
    void getMyCart_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));

        CartResponseDTO result = cartService.getMyCart();

        assertNotNull(result);
        assertEquals(1L, result.getCartId());
        assertEquals(BigDecimal.ZERO, result.getTotalPrice());
    }
}