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

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreatedOrderDto dto) {
        log.info("REST: Creating new order for user ID: {}", dto.getUserId());
        OrderDto created = orderService.createOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        log.info("REST: Fetching order with ID: {}", orderId);
        OrderDto order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<OrderDto>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST: Fetching orders for user {} - Page: {}, Size: {}", userId, page, size);
        Page<OrderDto> orders = orderService.getOrdersByUserId(userId, page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDto>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST: Fetching orders with status: {} - Page: {}, Size: {}", status, page, size);
        Page<OrderDto> orders = orderService.getOrdersByStatus(status, page, size);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        log.info("REST: Updating order {} status to {}", orderId, status);
        OrderDto updated = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long orderId) {
        log.info("REST: Cancelling order {}", orderId);
        OrderDto cancelled = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/user/{userId}/date-range")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<OrderDto>> getOrdersByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST: Fetching orders for user {} between {} and {}", userId, startDate, endDate);
        Page<OrderDto> orders = orderService.getOrdersByDateRange(userId, startDate, endDate, page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/product/{productId}/count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Long> getProductOrderCount(@PathVariable Long productId) {
        log.info("REST: Counting orders for product ID: {}", productId);
        Long count = orderService.getProductOrderCount(productId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/user/{userId}/exists")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Boolean> userHasOrders(@PathVariable Long userId) {
        log.info("REST: Checking if user {} has orders", userId);
        Page<OrderDto> orders = orderService.getOrdersByUserId(userId, 0, 1);
        boolean hasOrders = orders.getTotalElements() > 0;
        return ResponseEntity.ok(hasOrders);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        log.info("REST: Fetching order statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "Order statistics endpoint - implement as needed");

        return ResponseEntity.ok(stats);
    }
}
