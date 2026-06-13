package com.microservice_demo.demo_service_2.controller;

import com.microservice_demo.demo_service_2.dto.functionality.CreatedOrderDto;
import com.microservice_demo.demo_service_2.dto.functionality.OrderDto;
import com.microservice_demo.demo_service_2.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class OrderController {

    private final OrderService orderService;

    // USER + ADMIN endpoints

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreatedOrderDto dto) {
        log.info("[USER|ADMIN] Create order — userId={} products={}",
                dto.getUserId(), dto.getProductIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(dto));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        log.info("Get order — id={}", orderId);
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Page<OrderDto>> getOrdersByUserId(
            @PathVariable                      Long userId,
            @RequestParam(defaultValue = "0")  int  page,
            @RequestParam(defaultValue = "10") int  size) {
        log.info("Get orders by userId — userId={} page={} size={}", userId, page, size);
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, page, size));
    }

    @GetMapping("/user/{userId}/date-range")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Page<OrderDto>> getOrdersByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get orders by date range — userId={} start={} end={}", userId, startDate, endDate);
        return ResponseEntity.ok(
                orderService.getOrdersByDateRange(userId, startDate, endDate, page, size));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long orderId) {
        log.info("Cancel order — id={}", orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

//    ADMIN - only endpoint
@GetMapping("/status/{status}")
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<Page<OrderDto>> getOrdersByStatus(
        @PathVariable                      String status,
        @RequestParam(defaultValue = "0")  int    page,
        @RequestParam(defaultValue = "10") int    size) {
    log.info("[ADMIN] Get orders by status — status={} page={} size={}", status, page, size);
    return ResponseEntity.ok(orderService.getOrdersByStatus(status, page, size));
}

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long   orderId,
            @RequestParam String status) {
        log.info("[ADMIN] Update order status — id={} status={}", orderId, status);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        log.info("[ADMIN] Get order statistics");
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }

// Inter-service endpoints — PUBLIC (called by demo-service1 Feign, no JWT)
// SecurityConfig has these paths in permitAll() — no @PreAuthorize needed.

    @GetMapping("/product/{productId}/count")
    public ResponseEntity<Long> getProductOrderCount(@PathVariable Long productId) {
        log.info("[Feign] Product order count — productId={}", productId);
        return ResponseEntity.ok(orderService.getProductOrderCount(productId));
    }

    @GetMapping("/user/{userId}/exists")
    public ResponseEntity<Boolean> userHasOrders(@PathVariable Long userId) {
        log.info("[Feign] User has orders check — userId={}", userId);
        return ResponseEntity.ok(orderService.userHasOrders(userId));
    }
}


