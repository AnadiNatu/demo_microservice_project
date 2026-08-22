package com.microservice_demo.demo_service_2.service;

import com.microservice_demo.demo_service_2.aop.Stopwatch;
import com.microservice_demo.demo_service_2.dto.functionality.*;
import com.microservice_demo.demo_service_2.entity.Order;
import com.microservice_demo.demo_service_2.entity.OrderItem;
import com.microservice_demo.demo_service_2.entity.Users;
import com.microservice_demo.demo_service_2.enums.OrderStatus;
import com.microservice_demo.demo_service_2.exception.errors.BadRequestException;
import com.microservice_demo.demo_service_2.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_2.feign.DemoService1FeignClient;
import com.microservice_demo.demo_service_2.kafka.OrderEventProducer;
import com.microservice_demo.demo_service_2.repository.OrderRepository;
import com.microservice_demo.demo_service_2.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

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
    private final ApplicationContext applicationContext;
    private final OrderEventProducer orderEventProducer;

    private OrderService self() {
        return applicationContext.getBean(OrderService.class);
    }

//    @CacheEvict(value = "orders" , allEntries = true)
//    @CircuitBreaker(name = "demoService1", fallbackMethod = "createOrderFallback")
//    @Retry(name = "demoService1")
    @Stopwatch
    @Transactional
    @Caching(evict = { // added: invalidate list caches so the new order is visible immediately
            @CacheEvict(value = "userOrders", allEntries = true),
            @CacheEvict(value = "statusOrders", allEntries = true)
    })
    public OrderDto createOrder(CreatedOrderDto dto) {
        log.info("Creating order — userId={} itemCount={}", dto.getUserId(), dto.getItems().size());

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException(
                    "Order must contain at least one item"
            );
        }

        // 1. Validate user exists locally
        Users user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not synced in demo-service2. userId=" + dto.getUserId()));

        // 2. Extract productIds from items for the Feign call
//        List<Long> productIds = dto.getItems().stream()
//                .map(CreateOrderItemDto::getProductId)
//                .collect(Collectors.toList());

//        if (productIds.isEmpty()) {
//            throw new BadRequestException("Order must contain at least one item");
//        }

        for (CreateOrderItemDto item : dto.getItems()) {

            if (item.getProductId() == null) {
                throw new BadRequestException(
                        "Product ID cannot be null"
                );
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BadRequestException(
                        "Quantity must be greater than zero for productId="
                                + item.getProductId()
                );
            }
        }

        // 4. Extract product IDs
        List<Long> productIds = dto.getItems()
                .stream()
                .map(CreateOrderItemDto::getProductId)
                .distinct()
                .collect(Collectors.toList());

        return self().createOrderWithRemoteCalls(dto, user, productIds
        );
    }

    @CircuitBreaker(name = "demoService1", fallbackMethod = "createOrderWithRemoteCallsFallback")
    @Retry(name = "demoService1")
    @Transactional
    public OrderDto createOrderWithRemoteCalls(CreatedOrderDto dto, Users user, List<Long> productIds) {

        // Fetch products from DS1
        List<ProductInfoDto> products;
        try {
            products = demoService1Client.getProductsByIds(productIds);
        } catch (Exception ex) {
            log.error("Feign failed: getProductsByIds — {}", ex.getMessage());
            throw new RuntimeException("Product service unavailable. Try again later.");
        }

        if (products == null || products.size() != productIds.size()) {
            throw new BadRequestException("Some products not found in demo-service1. " +
                    "Requested=" + productIds.size() + " found=" + (products == null ? 0 : products.size()));
        }

        Map<Long, ProductInfoDto> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfoDto::getProductId, p -> p));

        // Validate stock and build OrderItems
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderItemDto itemDto : dto.getItems()) {
            Long productId = itemDto.getProductId();
            Integer quantity = itemDto.getQuantity();
            ProductInfoDto product = productMap.get(productId);

            if (product == null) {
                throw new ResourceNotFoundException("Product not found in demo-service1: id=" + productId);
            }
            Integer currentStock = product.getStockQuantity();

            if (currentStock == null) {
                throw new BadRequestException("Stock quantity is not initialized for productId=" + productId);
            }

            if (currentStock < quantity) {
                throw new BadRequestException("Insufficient stock for productId=" + productId + " available=" + currentStock + " requested=" + quantity);
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .productId(productId)
                    .productName(product.getProductName())
                    .category(product.getCategory())
                    .brand(product.getBrand())
                    .sku(product.getSku())
                    .creatorUserId(product.getCreatorUserId())
                    .creatorUsername(product.getCreatorUsername())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            orderItems.add(item);
        }

        // 4. DECREMENT STOCK IN DEMO-SERVICE
        log.info("[INVENTORY] Starting stock decrement for order | userId={}", dto.getUserId());

        for (CreateOrderItemDto itemDto : dto.getItems()) {
            Long productId = itemDto.getProductId();
            Integer quantity = itemDto.getQuantity();

            try {
                log.info("[INVENTORY] Decrementing stock | " + "productId={} quantity={}", productId, quantity);
                ProductInfoDto updatedProduct = demoService1Client.decrementProductStock(productId, quantity);
                log.info("[INVENTORY] Stock decrement successful | " + "productId={} quantity={} newStock={}", productId, quantity, updatedProduct.getStockQuantity());

            } catch (Exception ex) {
                log.error("[INVENTORY] Stock decrement FAILED | " + "productId={} quantity={}", productId, quantity, ex);
                throw new BadRequestException("Unable to decrement stock for productId=" + productId + ". Order was not created.");
            }
        }

        // 5. BUILD ORDER
        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId(dto.getUserId())
                .totalAmount(total)
                .orderStatus(OrderStatus.PENDING)
                .shippingName(dto.getShippingName())
                .shippingPhone(dto.getShippingPhone())
                .shippingEmail(dto.getShippingEmail())
                .shippingAddress(dto.getShippingAddress())
                .shippingCity(dto.getShippingCity())
                .shippingState(dto.getShippingState())
                .shippingCountry(dto.getShippingCountry())
                .postalCode(dto.getPostalCode())
                .notes(dto.getNotes())
                .orderDate(LocalDateTime.now())
                .build();

        // 6. CONNECT ORDER ITEMS
        orderItems.forEach(order::addItem);

        // 7. SAVE ORDER
        Order saved = orderRepository.save(order);

        log.info("[ORDER] Order created successfully | " + "orderId={} orderNumber={} total={}", saved.getOrderId(), saved.getOrderNumber(), saved.getTotalAmount());

        return toDto(saved);
    }

    @SuppressWarnings("unused")
    private OrderDto createOrderWithRemoteCallsFallback(CreatedOrderDto dto, Users user,
                                                        List<Long> productIds, Exception e) {
        log.error("Circuit breaker fallback for createOrder — {}", e.getMessage());
        throw new RuntimeException("Order service is currently unavailable. Please try again later.");
    }

//    @Stopwatch
//    @Cacheable(value = "orders" , key = "#orderId")
//    @CircuitBreaker(name = "demoService1" , fallbackMethod = "getOrderFallback")
//    public OrderDto getOrder(Long orderId){
//        log.info("Fetching order with ID : {}" , orderId);
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> {
//            log.error("Order not found : {}" , orderId);
//            return new ResourceNotFoundException("Order not found : " + orderId);
//        });
//
////        List<ProductInfoDto> products = null;
////        if (order.getProductIds() != null && !order.getProductIds().isEmpty()){
////            log.info("Fetching product details for order {}" , orderId);
////            products = demoService1Client.getProductsByIds(order.getProductIds());
////        }
//
//        log.info("Order found : {}" , order.getOrderNumber());
//        return toDto(order);
//    }

    @SuppressWarnings("unused")
    private OrderDto getOrderFallback(Long orderId, Exception e) {
        log.warn("[CircuitBreaker] Fallback triggered for getOrder - OrderID: {}, Error: {}", orderId, e.getMessage());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return toDto(order);
    }

    @Stopwatch
    @Cacheable(value = "userOrders", key = "#userId + '_' + #page + '_' + #size")
    public Page<OrderDto> getOrdersByUserId(Long userId, int page, int size) {
        log.info("Fetching orders for user ID: {} - Page: {}, Size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        log.info("Found {} orders for user ID: {}", orderPage.getTotalElements(), userId);
        return orderPage.map(order -> toDto(order));
    }

    @Transactional(readOnly = true)
    @Stopwatch
    @Cacheable(value = "statusOrders", key = "#status + '_' + #page + '_' + #size")
    public Page<OrderDto> getOrdersByStatus(String status, int page, int size) {
        log.info("Fetching orders with status: {} - Page: {}, Size: {}", status, page, size);

//        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        OrderStatus orderStatus = parseStatus(status);
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);

        log.info("Found {} orders with status: {}", orderPage.getTotalElements(), status);
        return orderPage.map(order -> toDto(order));
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
        return toDto(orderRepository.save(order));
    }

    @Stopwatch
    public Long getProductOrderCount(Long productId) {
        log.info("[Feign] Counting orders containing product ID: {}", productId);

        List<Order> allOrders = orderRepository.findAll();
        long count = allOrders.stream()
                .filter(order -> order.getItems() != null &&
                        order.getItems().stream().anyMatch(item -> productId.equals(item.getProductId())))
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
        return orderPage.map(order -> toDto(order));
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
        return toDto(updated);
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

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId) {
        log.info("Get order — id={}", orderId);
        Order order = orderRepository.findByIdWithItems(orderId)   // fetch-join version, see below
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toDto(order);
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

    private OrderDto toDto(Order order) {
        Users user = userRepository.findById(order.getUserId()).orElse(null);

        List<OrderItemDto> itemDtos = order.getItems() == null ? new ArrayList<>() :
                order.getItems().stream().map(item -> OrderItemDto.builder()
                                                      .orderItemId(item.getOrderItemId())
                                                      .productId(item.getProductId())
                                                      .productName(item.getProductName())
                                                      .category(item.getCategory())
                                                      .brand(item.getBrand())
                                                      .sku(item.getSku())
                                                      .creatorUserId(item.getCreatorUserId())
                                                      .creatorUsername(item.getCreatorUsername())
                                                      .quantity(item.getQuantity())
                                                      .unitPrice(item.getUnitPrice())
                                                      .subtotal(item.getSubtotal())
                                                      .build()
                ).collect(Collectors.toList());

        return OrderDto.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .username(user != null ? user.getName() : "Unknown")
                .orderStatus(order.getOrderStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(itemDtos)
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingEmail(order.getShippingEmail())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingCountry(order.getShippingCountry())
                .postalCode(order.getPostalCode())
                .notes(order.getNotes())
                .orderDate(order.getOrderDate())
                .createdOn(order.getCreatedOn())
                .updatedOn(order.getUpdatedOn())
                .shippedDate(order.getShippedDate())
                .estimatedDelivery(order.getEstimatedDelivery())
                .deliveryDate(order.getDeliveryDate())
                .cancelledDate(order.getCancelledDate())
                .build();
    }

    @Stopwatch
    @Transactional(readOnly = true)
    public List<OrderLogDto> getOrderLogsByProduct(String productName) {

        log.info("[ORDER-LOGS] START | productName={}", productName);

        StopWatch sw = new StopWatch("getOrderLogsByProduct");
        sw.start();

        String search = productName.trim().toLowerCase();

        List<OrderLogDto> result = orderRepository.findAll()
                .stream()
                .filter(order -> order.getItems() != null)
                .flatMap(order -> order.getItems().stream()
                        .filter(item -> item.getProductName() != null && item.getProductName().toLowerCase().contains(search))
                        .map(item -> OrderLogDto.builder()
                                .orderId(order.getOrderId())
                                .productName(item.getProductName())
                                .userName(resolveUserName(order.getUserId()))
                                .orderQuantity(item.getQuantity())
                                .orderPrice(item.getSubtotal())
                                .orderStatus(order.getOrderStatus().name())
                                .deliveredOn(order.getDeliveryDate())
                                .productInventory(null)
                                .productOrderQuantity(item.getQuantity())
                                .build()
                        )).toList();

        sw.stop();

        log.info(
                "[ORDER-LOGS] SUCCESS | productName={} | results={} | duration={}ms",
                productName,
                result.size(),
                sw.getTotalTimeMillis()
        );

        return result;
    }

    @Stopwatch
    @Transactional(readOnly = true)
    public List<OrderLogDto> getOrderLogsByUsers() {

        log.info("[ORDER-LOGS] START | all users");

        StopWatch sw = new StopWatch("getOrderLogsByUsers");
        sw.start();

        List<OrderLogDto> result = orderRepository.findAll()
                .stream()
                .filter(order -> order.getItems() != null)
                .flatMap(order -> order.getItems().stream()
                        .map(item -> OrderLogDto.builder()
                                .orderId(order.getOrderId())
                                .productName(item.getProductName())
                                .userName(resolveUserName(order.getUserId()))
                                .orderQuantity(item.getQuantity())
                                .orderPrice(item.getSubtotal())
                                .orderStatus(order.getOrderStatus().name())
                                .deliveredOn(order.getDeliveryDate())
                                .productInventory(null)
                                .productOrderQuantity(item.getQuantity())
                                .build()
                        )
                )
                .toList();

        sw.stop();
        log.info("[ORDER-LOGS] SUCCESS | results={} | duration={}ms", result.size(), sw.getTotalTimeMillis());
        return result;
    }

    private String resolveUserName(Long userId) {

        Users user = userRepository.findById(userId).orElse(null);

        return user != null
                ? user.getName()
                : "Unknown";
    }
}
