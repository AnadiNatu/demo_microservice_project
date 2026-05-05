package com.microservice_demo.demo_service_2.service;

import com.microservice_demo.demo_service_2.aop.Stopwatch;
import com.microservice_demo.demo_service_2.dto.functionality.CreatedOrderDto;
import com.microservice_demo.demo_service_2.dto.functionality.OrderDto;
import com.microservice_demo.demo_service_2.dto.functionality.ProductInfoDto;
import com.microservice_demo.demo_service_2.entity.Order;
import com.microservice_demo.demo_service_2.entity.Users;
import com.microservice_demo.demo_service_2.enums.OrderStatus;
import com.microservice_demo.demo_service_2.exception.errors.BadRequestException;
import com.microservice_demo.demo_service_2.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_2.feign.DemoService1FeignClient;
import com.microservice_demo.demo_service_2.repository.OrderRepository;
import com.microservice_demo.demo_service_2.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DemoService1FeignClient demoService1Client;

//    @CacheEvict(value = "orders" , allEntries = true)

//    @CircuitBreaker(name = "demoService1", fallbackMethod = "createOrderFallback")
//    @Retry(name = "demoService1")
    @Transactional
    public OrderDto createOrder(CreatedOrderDto dto){
        log.info("Creating order — userId={} products={}", dto.getUserId(), dto.getProductIds());

        Users user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not synced in demo-service2 . userId=" +dto.getUserId()));

        if (dto.getProductIds().size() != dto.getQuantities().size()){
            throw new BadRequestException("productIds and quantities size mismatch");
        }

        return createOrderWithRemoteCalls(dto , user);
    }

    @CircuitBreaker(name = "demoService1" ,fallbackMethod = "createOrderFallback")
    @Retry(name = "demoService1")
    @Transactional
    public OrderDto createOrderWithRemoteCalls(CreatedOrderDto dto , Users user){

        // Fetch products from DS1
        List<ProductInfoDto> products;
        try {
            products = demoService1Client.getProductsByIds(dto.getProductIds());
        } catch (Exception ex) {
            log.error("Feign failed: getProductsByIds — {}", ex.getMessage());
            throw new RuntimeException("Product service unavailable. Try again later.");
        }

        if (products == null || products.size() != dto.getProductIds().size()) {
            throw new BadRequestException("Some products not found in demo-service1");
        }

        Map<Long, ProductInfoDto> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfoDto::getProductId, p -> p));

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < dto.getProductIds().size(); i++) {
            Long pid = dto.getProductIds().get(i);
            Integer qty = dto.getQuantities().get(i);
            ProductInfoDto product = productMap.get(pid);

            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + pid);
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() < qty) {
                throw new BadRequestException("Insufficient stock for productId=" + pid);
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        // Persist the order
        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId(dto.getUserId())
                .productIds(new ArrayList<>(dto.getProductIds()))
                .quantities(new ArrayList<>(dto.getQuantities()))
                .totalAmount(total)
                .orderStatus(OrderStatus.PENDING)
                .shippingAddress(dto.getShippingAddress())
                .notes(dto.getNotes())
                .orderDate(LocalDateTime.now())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order saved — {}", saved.getOrderNumber());

        // Update stock in DS1
        for (int i = 0; i < dto.getProductIds().size(); i++) {
            Long pid = dto.getProductIds().get(i);
            Integer qty = dto.getQuantities().get(i);
            int newStock = productMap.get(pid).getStockQuantity() - qty;
            try {
                demoService1Client.updateProductStock(pid, newStock);
            } catch (Exception ex) {
                log.error("Stock update failed — rolling back order | pid={}", pid, ex);
                throw new RuntimeException("Stock update failed. Order rolled back.");
            }
        }

        return toDto(saved, products);
    }

    @SuppressWarnings("unused")
    private OrderDto createOrderFallback(CreatedOrderDto dto , Exception e){
        log.error("Fallback triggered for createOrder - Error : {}" , e.getMessage());
        throw new RuntimeException("Order service is currently unavailable . Please try again later.");
    }

    @Stopwatch
    @Cacheable(value = "orders" , key = "#orderId")
    @CircuitBreaker(name = "demoService1" , fallbackMethod = "getOrderFallback")
    public OrderDto getOrder(Long orderId){
        log.info("Fetching order with ID : {}" , orderId);

        Order order = orderRepository.findById(orderId).orElseThrow(() -> {
            log.error("Order not found : {}" , orderId);
            return new ResourceNotFoundException("Order not found : " + orderId);
        });

        List<ProductInfoDto> products = null;
        if (order.getProductIds() != null && !order.getProductIds().isEmpty()){
            log.info("Fetching product details for order {}" , orderId);
            products = demoService1Client.getProductsByIds(order.getProductIds());
        }

        log.info("Order found : {}" , order.getOrderNumber());
        return toDto(order, products);
    }

    @SuppressWarnings("unused")
    private OrderDto getOrderFallback(Long orderId, Exception e) {
        log.warn("[CircuitBreaker] Fallback triggered for getOrder - OrderID: {}, Error: {}", orderId, e.getMessage());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return toDto(order, null); // Return without product details
    }

    @Stopwatch
    @Cacheable(value = "userOrders", key = "#userId + '_' + #page + '_' + #size")
    public Page<OrderDto> getOrdersByUserId(Long userId, int page, int size) {
        log.info("Fetching orders for user ID: {} - Page: {}, Size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        log.info("Found {} orders for user ID: {}", orderPage.getTotalElements(), userId);
        return orderPage.map(order -> toDto(order, null));
    }

    @Stopwatch
    @Cacheable(value = "statusOrders", key = "#status + '_' + #page + '_' + #size")
    public Page<OrderDto> getOrdersByStatus(String status, int page, int size) {
        log.info("Fetching orders with status: {} - Page: {}, Size: {}", status, page, size);

        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);

        log.info("Found {} orders with status: {}", orderPage.getTotalElements(), status);
        return orderPage.map(order -> toDto(order, null));
    }

//    @CacheEvict(value = {"orders", "userOrders", "statusOrders"}, allEntries = true)
    @Stopwatch
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders" , key = "#orderId"),
            @CacheEvict(value = "userOrders" , allEntries = true),
            @CacheEvict(value = "statusOrders" , allEntries = true)
    })
    public OrderDto updateOrderStatus(Long orderId, String newStatus) {
//        log.info("Updating order {} status to: {}", orderId, newStatus);

        log.info("[ADMIN] Update order status - id={} status={}" , orderId , newStatus);
        Order order = requireOrder(orderId);
        OrderStatus status = parseStatus(newStatus);
        order.setOrderStatus(status);
        if (status == OrderStatus.DELIVERED){
            order.setDeliveryDate(LocalDateTime.now());
        }
        return toDto(orderRepository.save(order) , null);
    }

    @Stopwatch
    public Long getProductOrderCount(Long productId) {
        log.info("[Feign] Counting orders containing product ID: {}", productId);

        List<Order> allOrders = orderRepository.findAll();
        long count = allOrders.stream()
                .filter(order -> order.getProductIds() != null && order.getProductIds().contains(productId))
                .count();

        log.info("[Feign] Product {} has been ordered {} times", productId, count);
        return count;
    }

    @Stopwatch
    public Page<OrderDto> getOrdersByDateRange(Long userId, LocalDateTime startDate,
                                               LocalDateTime endDate, int page, int size) {
        log.info("Fetching orders for user {} between {} and {}", userId, startDate, endDate);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findUserOrdersBetweenDates(userId, startDate, endDate, pageable);

        log.info("Found {} orders in date range", orderPage.getTotalElements());
        return orderPage.map(order -> toDto(order, null));
    }

    @Stopwatch
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders" , key = "#orderId"),
            @CacheEvict(value = "userOrders" , allEntries = true),
            @CacheEvict(value = "statusOrders" , allEntries = true)
    })
    public OrderDto cancelOrder(Long orderId) {
        log.info("Cancelling order ID: {}", orderId);

        Order order = orderRepository.findById(orderId).orElseThrow(() -> {
                    log.error("Order not found: {}", orderId);
                    return new ResourceNotFoundException("Order not found: " + orderId);
                });

        if (order.getOrderStatus() == OrderStatus.DELIVERED ||
                order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.error("Cannot cancel order in status: {}", order.getOrderStatus());
            throw new BadRequestException("Cannot cancel order in status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        log.info("Order cancelled: {}", order.getOrderNumber());
        return toDto(updated, null);
    }

// User with orders
@Stopwatch
public Boolean userHasOrders(Long userId) {
    log.info("[Feign] Check user has orders — userId={}", userId);
    return orderRepository.countByUserId(userId) > 0;
}

    //    Order Statics
    @Stopwatch
    public Map<String, Object> getOrderStatistics() {
        log.info("[ADMIN] Computing order statistics");
        List<Order> all = orderRepository.findAll();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), all.stream()
                    .filter(o -> o.getOrderStatus() == s).count());
        }

        BigDecimal totalRevenue = all.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED
                        && o.getOrderStatus() != OrderStatus.REFUNDED)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOrders",  (long) all.size());
        stats.put("byStatus",     byStatus);
        stats.put("totalRevenue", totalRevenue);
        stats.put("generatedAt",  LocalDateTime.now());
        return stats;
    }

//    Private Helper
private Order requireOrder(Long id) {
    return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
}

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid order status '" + status + "'. Valid values: "
                            + Arrays.toString(OrderStatus.values()));
        }
    }

    private OrderDto toDto(Order order, List<ProductInfoDto> products) {
        Users user = userRepository.findById(order.getUserId()).orElse(null);

        return OrderDto.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .username(user != null ? user.getName() : "Unknown")
                .productIds(order.getProductIds())
                .productDetails(products)
                .quantities(order.getQuantities())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus().name())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .orderDate(order.getOrderDate())
                .deliveryDate(order.getDeliveryDate())
                .createdOn(order.getCreatedOn())
                .updatedOn(order.getUpdatedOn())
                .build();
    }
}
