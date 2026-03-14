package com.ecommerce.ecommerce_backend.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecommerce_backend.dto.request.ProductRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.ProductResponseDTO;
import com.ecommerce.ecommerce_backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Public product browsing with pagination and filtering. Admin-only create, update, delete.")
public class ProductController {

    @Autowired
    private ProductService productService;

    // create Product(Admin only)
    @Operation(summary = "Create a product (Admin only)",
               security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(@Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED.value(), "Product created successfully", response));
    }

    // Update Product(Admin only)
    @Operation(summary = "Update a product (Admin only)",
               security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDTO request) {

        ProductResponseDTO response = productService.updateProduct(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Product updated successfully", response));
    }

    // Delete Product(Admin only)
    @Operation(summary = "Delete a product (Admin only)",
               security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Product deleted successfully", null));
    }

    // get Product by ID (Public)
    @Operation(summary = "Get product by ID",
               description = "Public endpoint — no token required.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(@PathVariable Long id) {
        ProductResponseDTO response = productService.getProductById(id);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Product fetched successfully", response));
    }

    // Get All Products with optional filters (Public)
    @Operation(summary = "Get all products",
               description = """
                       Public endpoint — no token required.
                       Supports pagination and optional filters:
                       - `category` — filter by category name (case-insensitive)
                       - `minPrice` / `maxPrice` — filter by price range
                       - `page`, `size` — pagination (default: page=0, size=10)
                       - `sortBy`, `sortDir` — sorting (default: id, asc)
                       """)
    @GetMapping()
    public ResponseEntity<ApiResponse<Page<ProductResponseDTO>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<ProductResponseDTO> products = productService.getAllProducts(
                category, minPrice, maxPrice, page, size, sortBy, sortDir);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Products fetched successfully", products));
    }

}
