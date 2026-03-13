package com.ecommerce.ecommerce_backend.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecommerce_backend.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
     // Filter by category 
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    // Filter by price range
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Filter by category AND price range together
    Page<Product> findByCategoryIgnoreCaseAndPriceBetween(
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable);

    // Search by name 
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
