package com.microservice_demo.demo_service_1.controller;

import com.microservice_demo.demo_service_1.dto.functionality.CreateProductDto;
import com.microservice_demo.demo_service_1.dto.functionality.ProductDto;
import com.microservice_demo.demo_service_1.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class ProductController {

    private final ProductService productService;


    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductDto dto) {
        log.info("REST: Creating product - {}", dto.getProductName());
        ProductDto created = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long productId) {
        log.info("REST: Fetching product with ID: {}", productId);
        ProductDto product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy) {
        log.info("REST: Fetching all products - Page: {}, Size: {}", page, size);
        Page<ProductDto> products = productService.getAllProducts(page, size, sortBy);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> getActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST: Fetching active products - Page: {}, Size: {}", page, size);
        Page<ProductDto> products = productService.getActiveProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST: Fetching products by category: {} - Page: {}, Size: {}", category, page, size);
        Page<ProductDto> products = productService.getProductsByCategory(category, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST: Searching products with keyword: {} - Page: {}, Size: {}", keyword, page, size);
        Page<ProductDto> products = productService.searchProducts(keyword, page, size);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{productId}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        log.info("REST: Updating stock for product {} to {}", productId, quantity);
        ProductDto updated = productService.updateStock(productId, quantity);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{productId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> deactivateProduct(@PathVariable Long productId) {
        log.info("REST: Deactivating product {}", productId);
        ProductDto deactivated = productService.deactivateProduct(productId);
        return ResponseEntity.ok(deactivated);
    }

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ProductDto>> getProductsByIds(@RequestBody List<Long> productIds) {
        log.info("REST: Fetching products by IDs: {}", productIds);
        List<ProductDto> products = productService.getProductsByIds(productIds);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}/order-stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductOrderStats(@PathVariable Long productId) {
        log.info("REST: Fetching order stats for product {}", productId);

        ProductDto product = productService.getProduct(productId);
        Long orderCount = productService.getProductOrderCount(productId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("product", product);
        stats.put("totalOrders", orderCount);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{productId}/available")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Boolean> isProductAvailable(@PathVariable Long productId) {
        log.info("REST: Checking availability for product {}", productId);
        ProductDto product = productService.getProduct(productId);
        boolean available = product.getActive() && product.getStockQuantity() > 0;
        return ResponseEntity.ok(available);
    }

}
