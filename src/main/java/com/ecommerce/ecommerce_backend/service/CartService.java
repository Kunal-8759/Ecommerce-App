package com.ecommerce.ecommerce_backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ecommerce.ecommerce_backend.dto.request.AddToCartRequestDTO;
import com.ecommerce.ecommerce_backend.dto.request.UpdateCartItemRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.CartItemResponseDTO;
import com.ecommerce.ecommerce_backend.dto.response.CartResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Cart;
import com.ecommerce.ecommerce_backend.entity.CartItem;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.InsufficientStockException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.CartItemRepository;
import com.ecommerce.ecommerce_backend.repository.CartRepository;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;


    /*
    Get Logged in User from Security Context
    JWT filter already Validated the token and set the User in the Context.
    We retrieve it here so every cart operation is tied to the right user.
    */
    private User getLoggedInUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName(); // getName() returns the email (set as principal in JwtAuthFilter)

        log.debug("Resolving logged-in user from SecurityContext — email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() ->{
                    log.warn("Logged-in user not found with email: {}", email);
                    return new ResourceNotFoundException("Logged-in user not found with email: " + email);
                });
    }
    /*
    Get or Create Cart for logged-in user
        - If the user already has a cart, return it.
        - If not, create a new cart with totalPrice = 0 and return it.
        This ensures that every user always has a cart to work with when they add items.
    */

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("No existing cart found for user id: {}. Creating new cart.", user.getId());
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setTotalPrice(BigDecimal.ZERO);
                    return cartRepository.save(newCart);
                });
    }

    /*
    Recalculate total price of cart
        - Whenever items are added, updated, or removed from the cart, we need to recalculate the total price.
        - This method sums up the price of each cart item (product price * quantity) and updates the cart's totalPrice.
    */
    private void recalculateTotal(Cart cart) {
        BigDecimal total = cart.getCartItems()
                .stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
        cartRepository.save(cart);

        log.debug("Cart total recalculated — cartId: {}, newTotal: {}", cart.getId(), total);
    }

    /*
    Add product to cart
     - If the product is already in the cart, just increase the quantity.
     - If not, create a new CartItem and add it to the cart.
     - Before adding, check if the requested quantity is available in stock.
     - After adding, recalculate the total price of the cart.
     - Return the updated cart details as CartResponseDTO.
     */
    public CartResponseDTO addToCart(AddToCartRequestDTO request) {
        User user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        log.debug("Adding to cart — userId: {}, productId: {}, requestedQty: {}",
                user.getId(), request.getProductId(), request.getQuantity());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->{
                    log.warn("Add to cart failed — product not found with id: {}", request.getProductId());
                    return new ResourceNotFoundException("Product not found with id: " + request.getProductId());
                });

        // Check stock availability before adding
        if (product.getStock() < request.getQuantity()) {
            log.warn("Insufficient stock for product id: {} — available: {}, requested: {}",
                product.getId(), product.getStock(), request.getQuantity());
            throw new InsufficientStockException("Insufficient stock for '" + product.getName() + "'. Available: " + product.getStock());
        }


        // If product already in cart → just increase quantity
        // If not → create a new CartItem
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (cartItem != null) {
            int updatedQty = cartItem.getQuantity() + request.getQuantity();

            // Re-check stock for the updated combined quantity
            if (product.getStock() < updatedQty) {
                log.warn("Cannot add to cart — insufficient stock for product id: {} after quantity update. Available: {}, requested: {}",
                    product.getId(), product.getStock(), updatedQty);
                throw new InsufficientStockException("Cannot add more. Insufficient stock for '" + product.getName() + "'. Available: " + product.getStock());
            }
            cartItem.setQuantity(updatedQty);

            log.info("Cart item quantity increased — userId: {}, productId: {}, " +
                    "oldQty: {}, newQty: {}",
                    user.getId(), product.getId(),
                    cartItem.getQuantity() - request.getQuantity(), updatedQty);
        } else {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cart.getCartItems().add(cartItem);

            log.info("New item added to cart — userId: {}, productId: {}, " +
                    "productName: {}, quantity: {}",
                    user.getId(), product.getId(),
                    product.getName(), request.getQuantity());
        }

        cartItemRepository.save(cartItem);
        log.info("Product added to cart — userId: {}, productId: {}, quantity: {}",
            user.getId(), product.getId(), request.getQuantity());

        recalculateTotal(cart);

        return mapToCartResponseDTO(cart);
    }


    /*
        Update quantity of a product in the cart
        - Find the CartItem for the given product in the user's cart.
        - If not found, throw an exception (user should add it first).
        - Validate the new quantity against available stock.
        - Update the quantity and save the CartItem.
        - Recalculate the cart's total price.
        - Return the updated cart details as CartResponseDTO.
    */
    public CartResponseDTO updateCartItem(Long productId, UpdateCartItemRequestDTO request) {
        User user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        log.debug("Updating cart item — userId: {}, productId: {}, newQty: {}",
                user.getId(), productId, request.getQuantity());

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> {
                    log.warn("Product not found in cart — userId: {}, productId: {}", user.getId(), productId);
                    return new ResourceNotFoundException("Product not found in cart. Add it first.");
                });

        Product product = cartItem.getProduct();

        // Validate new quantity against stock
        if (product.getStock() < request.getQuantity()) {
            log.warn("Insufficient stock for product id: {} — available: {}, requested: {}",
                product.getId(), product.getStock(), request.getQuantity());
            throw new InsufficientStockException("Insufficient stock for '" + product.getName() + "'. Available: " + product.getStock());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        recalculateTotal(cart);

        log.info("Cart item quantity updated — userId: {}, productId: {}, " +
                "newQty: {}",
                user.getId(), product.getId(), request.getQuantity());

        return mapToCartResponseDTO(cart);
    }

    /*
        Remove a product from the cart
        - Find the CartItem for the given product in the user's cart.
        - Remove the CartItem from the cart.
        - Save the updated cart.
        - Recalculate the cart's total price.
        - Return the updated cart details as CartResponseDTO.

    */
    public CartResponseDTO removeFromCart(Long productId) {
        User user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        log.debug("Removing item from cart — userId: {}, productId: {}",
                user.getId(), productId);

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> {
                    log.warn("Product not found in cart — userId: {}, productId: {}", user.getId(), productId);
                    return new ResourceNotFoundException("Product not found in cart");
                });

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        recalculateTotal(cart);

        log.info("Product removed from cart — userId: {}, productId: {}",
                user.getId(), productId);

        return mapToCartResponseDTO(cart);
    }

    /*
        View the user's cart
        - Get the logged-in user.
        - Retrieve or create the user's cart.
        - Return the cart details as CartResponseDTO.
    */
    public CartResponseDTO getMyCart() {
        User user = getLoggedInUser();

        log.debug("Fetching cart for userId: {}", user.getId());

        Cart cart = getOrCreateCart(user);
        return mapToCartResponseDTO(cart);
    }

    /*
        Clear the user's cart
        - Clear all items from the cart.
        - Set the cart's total price to zero.
        - Save the updated cart.
    */
    public void clearCart(Cart cart) {

        log.info("Clearing cart after successful payment — cartId: {}, userId: {}",
                cart.getId(), cart.getUser().getId());

        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info("Cart cleared successfully — cartId: {}", cart.getId());
    }

    /*
        Map Cart → CartResponseDTO
        - Convert the Cart entity to a CartResponseDTO for API responses.
        - This includes mapping each CartItem to CartItemResponseDTO and calculating subtotals.
    */
    private CartResponseDTO mapToCartResponseDTO(Cart cart) {
        List<CartItemResponseDTO> itemDTOs = cart.getCartItems()
                .stream()
                .map(this::mapToCartItemResponseDTO)
                .collect(Collectors.toList());

        CartResponseDTO response = new CartResponseDTO();
        response.setCartId(cart.getId());
        response.setUserId(cart.getUser().getId());
        response.setCartItems(itemDTOs);
        response.setTotalPrice(cart.getTotalPrice());

        return response;
    }

    /*
        Map CartItem → CartItemResponseDTO
        - Convert a CartItem entity to CartItemResponseDTO for API responses.
        - This includes product details and calculating the subtotal for the item.
    */
    private CartItemResponseDTO mapToCartItemResponseDTO(CartItem item) {
        CartItemResponseDTO dto = new CartItemResponseDTO();
        dto.setCartItemId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setProductPrice(item.getProduct().getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(
            item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
        );
        return dto;
    }

}
