package com.microservice_demo.demo_service_2.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DemoService1FeignClient demoService1Client;

    @Transactional
    @CacheEvict(value = "orders" , allEntries = true)
    @CircuitBreaker(name = "demoService1" , fallbackMethod = "createOrderFallback")
    @Retry(name = "demoService1")
    public OrderDto createOrder(CreatedOrderDto dto){
        log.info("Creating new order for user ID : {}"  , dto.getUserId());

        Users user = userRepository.findById(dto.getUserId()).orElseThrow(() -> {
            log.error("Users not found : {} " , dto.getUserId());
            return new ResourceNotFoundException("User not found");
        });

        if (dto.getProductIds().size() != dto.getQuantities().size()) {
            log.error("Product IDs and quantities count mismatch");
            throw new BadRequestException("Product IDs and quantities must have the same count");
        }

        log.info("Fetching product details from Demo-Service1 for product IDs : {}" , dto.getUserId());
        List<ProductInfoDto> products = demoService1Client.getProductsByIds(dto.getProductIds());

        if (products == null || products.isEmpty()){
            log.error("No products found for the given IDs");
            throw new BadRequestException("No products found for the given IDs");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int i = 0 ; i < products.size() ; i++) {
            ProductInfoDto product = products.get(i);
            Integer quantity = dto.getQuantities().get(i);

            if (product.getStockQuantity() < quantity) {
                log.error("Insufficient stock for product : {} - Available : {} , Requested : {}", product.getProductName(), product.getStockQuantity(), quantity);
                throw new BadRequestException("Insufficient stock for product");
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemTotal);
        }
            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0 , 8).toUpperCase();

            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .userId(dto.getUserId())
                    .productIds(new ArrayList<>(dto.getProductIds()))
                    .quantities(new ArrayList<>(dto.getQuantities()))
                    .totalAmount(totalAmount)
                    .orderStatus(OrderStatus.PENDING)
                    .shippingAddress(dto.getShippingAddress())
                    .notes(dto.getNotes())
                    .build();

            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully - Order Number : {} , Total : {}" , orderNumber , totalAmount);

            try{
                for (int i = 0 ; i < products.size() ; i++){
                    Long productIds = products.get(i).getProductId();
                    Integer newStock = products.get(i).getStockQuantity();
                    log.info("Updating stock for product ID : {} to {}" , productIds, newStock );
                    demoService1Client.updateProductStock(productIds ,  newStock);
                }
            }catch (Exception ex){
                log.error("Failed to update product stocks : {}" , ex.getMessage());
            }

            return toDto(savedOrder , products);
    }

    private OrderDto createOrderFallback(CreatedOrderDto dto , Exception e){
        log.error("Fallback triggered for createOrder - Error : {}" , e.getMessage());
        throw new RuntimeException("Order service is currently unavailable . Please try again later.");
    }

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

    private OrderDto getOrderFallback(Long orderId, Exception e) {
        log.warn("Fallback triggered for getOrder - OrderID: {}, Error: {}", orderId, e.getMessage());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return toDto(order, null); // Return without product details
    }

    @Cacheable(value = "userOrders", key = "#userId + '_' + #page + '_' + #size")
    public Page<OrderDto> getOrdersByUserId(Long userId, int page, int size) {
        log.info("Fetching orders for user ID: {} - Page: {}, Size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        log.info("Found {} orders for user ID: {}", orderPage.getTotalElements(), userId);
        return orderPage.map(order -> toDto(order, null));
    }

    @Cacheable(value = "statusOrders", key = "#status + '_' + #page + '_' + #size")
    public Page<OrderDto> getOrdersByStatus(String status, int page, int size) {
        log.info("Fetching orders with status: {} - Page: {}, Size: {}", status, page, size);

        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);

        log.info("Found {} orders with status: {}", orderPage.getTotalElements(), status);
        return orderPage.map(order -> toDto(order, null));
    }

    @Transactional
    @CacheEvict(value = {"orders", "userOrders", "statusOrders"}, allEntries = true)
    public OrderDto updateOrderStatus(Long orderId, String newStatus) {
        log.info("Updating order {} status to: {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found: {}", orderId);
                    return new ResourceNotFoundException("Order not found: " + orderId);
                });

        OrderStatus orderStatus = OrderStatus.valueOf(newStatus.toUpperCase());
        order.setOrderStatus(orderStatus);

        if (orderStatus == OrderStatus.DELIVERED) {
            order.setDeliveryDate(LocalDateTime.now());
        }

        Order updated = orderRepository.save(order);
        log.info("Order status updated: {} -> {}", order.getOrderNumber(), newStatus);

        return toDto(updated, null);
    }

    public Long getProductOrderCount(Long productId) {
        log.info("Counting orders containing product ID: {}", productId);

        List<Order> allOrders = orderRepository.findAll();
        long count = allOrders.stream()
                .filter(order -> order.getProductIds() != null && order.getProductIds().contains(productId))
                .count();

        log.info("Product {} has been ordered {} times", productId, count);
        return count;
    }

    public Page<OrderDto> getOrdersByDateRange(Long userId, LocalDateTime startDate,
                                               LocalDateTime endDate, int page, int size) {
        log.info("Fetching orders for user {} between {} and {}", userId, startDate, endDate);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findUserOrdersBetweenDates(userId, startDate, endDate, pageable);

        log.info("Found {} orders in date range", orderPage.getTotalElements());
        return orderPage.map(order -> toDto(order, null));
    }

    @Transactional
    @CacheEvict(value = {"orders", "userOrders", "statusOrders"}, allEntries = true)
    public OrderDto cancelOrder(Long orderId) {
        log.info("Cancelling order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
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
