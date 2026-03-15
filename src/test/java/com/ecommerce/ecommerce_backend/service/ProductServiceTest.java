package com.ecommerce.ecommerce_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.ecommerce.ecommerce_backend.dto.request.ProductRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.ProductResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private ProductService productService;

    private Product mockProduct;
    private ProductRequestDTO productRequest;
    private ProductResponseDTO mockProductResponse;

    @BeforeEach
    void setUp() {
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Samsung Galaxy S24");
        mockProduct.setPrice(new BigDecimal("79999.00"));
        mockProduct.setStock(50);
        mockProduct.setCategory("Electronics");

        productRequest = new ProductRequestDTO();
        productRequest.setName("Samsung Galaxy S24");
        productRequest.setPrice(new BigDecimal("79999.00"));
        productRequest.setStock(50);
        productRequest.setCategory("Electronics");

        mockProductResponse = new ProductResponseDTO();
        mockProductResponse.setId(1L);
        mockProductResponse.setName("Samsung Galaxy S24");
        mockProductResponse.setPrice(new BigDecimal("79999.00"));
        mockProductResponse.setStock(50);
    }

    //  Create Product 

    @Test
    void createProduct_Success() {
        when(modelMapper.map(productRequest, Product.class)).thenReturn(mockProduct);
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);
        when(modelMapper.map(mockProduct, ProductResponseDTO.class))
                .thenReturn(mockProductResponse);

        ProductResponseDTO result = productService.createProduct(productRequest);

        assertNotNull(result);
        assertEquals("Samsung Galaxy S24", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    //  Get Product By ID 

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(modelMapper.map(mockProduct, ProductResponseDTO.class))
                .thenReturn(mockProductResponse);

        ProductResponseDTO result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getProductById_ThrowsNotFoundException_WhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(99L));

        assertTrue(exception.getMessage().contains("99"));
    }

    //  Update Product 

    @Test
    void updateProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);
        when(modelMapper.map(mockProduct, ProductResponseDTO.class))
                .thenReturn(mockProductResponse);

        ProductResponseDTO result = productService.updateProduct(1L, productRequest);

        assertNotNull(result);
        verify(productRepository, times(1)).save(mockProduct);
    }

    @Test
    void updateProduct_ThrowsNotFoundException_WhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(99L, productRequest));
    }

    //  Delete Product 

    @Test
    void deleteProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteProduct_ThrowsNotFoundException_WhenProductDoesNotExist() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(99L));

        verify(productRepository, never()).deleteById(any());
    }
}