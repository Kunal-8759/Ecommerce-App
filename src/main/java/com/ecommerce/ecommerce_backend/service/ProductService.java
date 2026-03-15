package com.ecommerce.ecommerce_backend.service;

import java.math.BigDecimal;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    // create Product(Admin only)
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        log.info("Creating product: name={}, category={}, price={}",
                request.getName(), request.getCategory(), request.getPrice());

        Product product = modelMapper.map(request, Product.class);
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully — id: {}, name: {}", savedProduct.getId(), savedProduct.getName());
        return modelMapper.map(savedProduct, ProductResponseDTO.class);
    }

    // Update Product(Admin only)
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request) {
        log.info("Updating product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product update failed — product not found with id: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setRating(request.getRating());

        Product updated = productRepository.save(product);
        log.info("Product updated successfully — id: {}, name: {}", updated.getId(), updated.getName());
        return modelMapper.map(updated, ProductResponseDTO.class);
    }

    // Delete Product(Admin only)
    public void deleteProduct(Long id) {
        log.info("Deleting product id: {}", id);

        if (!productRepository.existsById(id)) {
            log.warn("Product delete failed — product not found with id: {}", id);
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted successfully — id: {}", id);
    }

    // Get Product by ID (Public)
    public ProductResponseDTO getProductById(Long id) {
        log.info("Fetching product id: {}", id);

        Product product = productRepository.findById(id).orElseThrow(() -> {
            log.warn("Product not found with id: {}", id);
            return new ResourceNotFoundException("Product not found with id: " + id);
        });

        log.info("Product fetched successfully — id: {}, name: {}", product.getId(), product.getName());
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

        log.info("Fetching products with filters — category: {}, price: {}-{}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                category, minPrice, maxPrice, page, size, sortBy, sortDir);

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

        log.debug("Products fetched — total: {}", products.getTotalElements());
        return products.map(product -> modelMapper.map(product, ProductResponseDTO.class));
    }
}