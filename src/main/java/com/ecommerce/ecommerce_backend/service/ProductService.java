package com.ecommerce.ecommerce_backend.service;

import java.math.BigDecimal;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ecommerce.ecommerce_backend.dto.request.ProductRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.ProductResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Product;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    // create Product(Admin only)
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Product product = modelMapper.map(request, Product.class);
        Product savedProduct = productRepository.save(product);
        return modelMapper.map(savedProduct, ProductResponseDTO.class);
    }

    // Update Product(Admin only)
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setRating(request.getRating());

        Product updated = productRepository.save(product);
        return modelMapper.map(updated, ProductResponseDTO.class);
    }

    // Delete Product(Admin only)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    // Get Product by ID (Public)
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        return modelMapper.map(product, ProductResponseDTO.class);
    }

    // Get All Products with pagination and filtering (Public)
     public Page<ProductResponseDTO> getAllProducts(
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products;

        boolean hasCategory = category != null && !category.isBlank();
        boolean hasPrice = minPrice != null && maxPrice != null;

        if (hasCategory && hasPrice) {
            products = productRepository.findByCategoryIgnoreCaseAndPriceBetween(category, minPrice, maxPrice, pageable);
        } else if (hasCategory) {
            products = productRepository.findByCategoryIgnoreCase(category, pageable);
        } else if (hasPrice) {
            products = productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        } else {
            // No filters — return everything paginated
            products = productRepository.findAll(pageable);
        }

        return products.map(product -> modelMapper.map(product, ProductResponseDTO.class));
    }
}