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
import org.springframework.web.multipart.MultipartFile;

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

    // ========== ADMIN-ONLY ENDPOINTS ==========

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductDto dto) {
        log.info("[ProductController] [ADMIN] Creating product: {}", dto.getProductName());
        ProductDto created = productService.createProduct(dto);
        log.info("[ProductController] ✅ Product created with ID: {}", created.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy) {
        log.info("[ProductController] [ADMIN] Get all products - page={} size={} sortBy={}", page, size, sortBy);
        Page<ProductDto> products = productService.getAllProducts(page, size, sortBy);
        log.info("[ProductController] ✅ Found {} products", products.getTotalElements());
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{productId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> deactivateProduct(@PathVariable Long productId) {
        log.info("[ProductController] [ADMIN] Deactivating product ID: {}", productId);
        ProductDto deactivated = productService.deactivateProduct(productId);
        log.info("[ProductController] ✅ Product deactivated");
        return ResponseEntity.ok(deactivated);
    }


    @PostMapping("/{productId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        log.info("[ProductController] [ADMIN] Upload image for productId={}", productId);
        ProductDto updated = productService.uploadProductImage(productId, file);
        log.info("[ProductController] Image uploaded | productId={} | url={}",
                productId, updated.getImageUrl());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{productId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> removeProductImage(@PathVariable Long productId) {
        log.info("[ProductController] [ADMIN] Remove image for productId={}", productId);
        ProductDto updated = productService.removeProductImage(productId);
        log.info("[ProductController] Image removed | productId={}", productId);
        return ResponseEntity.ok(updated);
    }

    // ========== USER + ADMIN ENDPOINTS ==========
    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long productId) {
        log.info("[ProductController] Get product ID: {}", productId);
        ProductDto product = productService.getProduct(productId);
        log.info("[ProductController] ✅ Product found: {}", product.getProductName());
        return ResponseEntity.ok(product);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> getActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("[ProductController] Get active products - page={} size={}", page, size);
        Page<ProductDto> products = productService.getActiveProducts(page, size);
        log.info("[ProductController] ✅ Found {} active products", products.getTotalElements());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("[ProductController] Get products by category: {} - page={} size={}", category, page, size);
        Page<ProductDto> products = productService.getProductsByCategory(category, page, size);
        log.info("[ProductController] ✅ Found {} products in category '{}'", products.getTotalElements(), category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("[ProductController] Search products - keyword='{}' page={} size={}", keyword, page, size);
        Page<ProductDto> products = productService.searchProducts(keyword, page, size);
        log.info("[ProductController] ✅ Found {} products matching '{}'", products.getTotalElements(), keyword);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{productId}/stock")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        log.info("[ProductController] Update stock - productId={} quantity={}", productId, quantity);
        ProductDto updated = productService.updateStock(productId, quantity);
        log.info("[ProductController] ✅ Stock updated successfully");
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ProductDto>> getProductsByIds(@RequestBody List<Long> productIds) {
        log.info("[ProductController] Batch fetch products - IDs: {}", productIds);
        List<ProductDto> products = productService.getProductsByIds(productIds);
        log.info("[ProductController] ✅ Found {} products out of {} requested", products.size(), productIds.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}/available")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Boolean> isProductAvailable(@PathVariable Long productId) {
        log.info("[ProductController] Check availability - productId={}", productId);
        ProductDto product = productService.getProduct(productId);
        boolean available = Boolean.TRUE.equals(product.getActive())
                && product.getStockQuantity() != null
                && product.getStockQuantity() > 0;
        log.info("[ProductController] ✅ Product availability: {}", available);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/{productId}/order-stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductOrderStats(@PathVariable Long productId) {
        log.info("[ProductController] Get order stats — productId={}", productId);
        ProductDto product = productService.getProduct(productId);
        Long orderCount = productService.getProductOrderCount(productId);

        Map<String, Object> response = new HashMap<>();
        response.put("product", product);
        response.put("totalOrders", orderCount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/order-stats/batch")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getBatchProductOrderStats(
            @RequestBody List<Long> productIds) {
        log.info("[ProductController] Batch order stats — productIds={}", productIds);
        Map<String, Object> result = new HashMap<>();
        for (Long pid : productIds) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("product", productService.getProduct(pid));
            entry.put("totalOrders", productService.getProductOrderCount(pid));
            result.put(String.valueOf(pid), entry);
        }
        return ResponseEntity.ok(result);
    }
}