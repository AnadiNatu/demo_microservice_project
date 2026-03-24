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
        // ADMIN-only endpoints
        @PostMapping
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")
        public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductDto dto) {
            log.info("[ADMIN] Create product — name='{}'", dto.getProductName());
            return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
        }

        @GetMapping
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")
        public ResponseEntity<Page<ProductDto>> getAllProducts(
                @RequestParam(defaultValue = "0")         int    page,
                @RequestParam(defaultValue = "10")        int    size,
                @RequestParam(defaultValue = "createdOn") String sortBy) {
            log.info("[ADMIN] Get all products — page={} size={} sortBy={}", page, size, sortBy);
            return ResponseEntity.ok(productService.getAllProducts(page, size, sortBy));
        }

        @PutMapping("/{productId}/deactivate")
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")
        public ResponseEntity<ProductDto> deactivateProduct(@PathVariable Long productId) {
            log.info("[ADMIN] Deactivate product — id={}", productId);
            return ResponseEntity.ok(productService.deactivateProduct(productId));
        }

        // USER + ADMIN endpoints
        @GetMapping("/{productId}")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<ProductDto> getProduct(@PathVariable Long productId) {
            log.info("Get product — id={}", productId);
            return ResponseEntity.ok(productService.getProduct(productId));
        }

        @GetMapping("/active")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<Page<ProductDto>> getActiveProducts(
                @RequestParam(defaultValue = "0")  int page,
                @RequestParam(defaultValue = "10") int size) {
            log.info("Get active products — page={} size={}", page, size);
            return ResponseEntity.ok(productService.getActiveProducts(page, size));
        }

        @GetMapping("/category/{category}")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<Page<ProductDto>> getProductsByCategory(
                @PathVariable String category,
                @RequestParam(defaultValue = "0")int page,
                @RequestParam(defaultValue = "10") int size) {
            log.info("Get products by category — category='{}' page={} size={}", category, page, size);
            return ResponseEntity.ok(productService.getProductsByCategory(category, page, size));
        }

        @GetMapping("/search")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<Page<ProductDto>> searchProducts(
                @RequestParam String keyword,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {
            log.info("Search products — keyword='{}' page={} size={}", keyword, page, size);
            return ResponseEntity.ok(productService.searchProducts(keyword, page, size));
        }

        @PutMapping("/{productId}/stock")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<ProductDto> updateStock(
                @PathVariable Long productId,
                @RequestParam Integer quantity) {
            log.info("Update stock — id={} qty={}", productId, quantity);
            return ResponseEntity.ok(productService.updateStock(productId, quantity));
        }

        @PostMapping("/list")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<List<ProductDto>> getProductsByIds(@RequestBody List<Long> productIds) {
            log.info("Batch fetch products — ids={}", productIds);
            return ResponseEntity.ok(productService.getProductsByIds(productIds));
        }

        @GetMapping("/{productId}/available")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<Boolean> isProductAvailable(@PathVariable Long productId) {
            log.info("Check availability — id={}", productId);
            ProductDto p = productService.getProduct(productId);
            boolean available = Boolean.TRUE.equals(p.getActive())
                    && p.getStockQuantity() != null
                    && p.getStockQuantity() > 0;
            return ResponseEntity.ok(available);
        }

        @GetMapping("/{productId}/order-stats")
        @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
        public ResponseEntity<Map<String, Object>> getProductOrderStats(@PathVariable Long productId) {
            log.info("Get order stats — productId={}", productId);
            ProductDto product    = productService.getProduct(productId);
            Long       orderCount = productService.getProductOrderCount(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("product",     product);
            response.put("totalOrders", orderCount);
            return ResponseEntity.ok(response);
        }
}
