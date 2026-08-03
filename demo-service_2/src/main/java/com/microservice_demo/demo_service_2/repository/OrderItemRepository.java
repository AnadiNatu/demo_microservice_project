package com.microservice_demo.demo_service_2.repository;


import com.microservice_demo.demo_service_2.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderOrderId(Long orderId);

    Page<OrderItem> findByProductId(Long productId, Pageable pageable);

    Page<OrderItem> findByCategory(String category, Pageable pageable);

    Page<OrderItem> findByCreatorUserId(Long creatorUserId, Pageable pageable);

    @Query("""
            SELECT oi
            FROM OrderItem oi
            WHERE oi.productName LIKE %:keyword%
            """)
    Page<OrderItem> searchByProductName(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT SUM(oi.quantity)
            FROM OrderItem oi
            WHERE oi.productId = :productId
            """)
    Integer getTotalSoldQuantity(@Param("productId") Long productId);

    @Query("""
            SELECT SUM(oi.subtotal)
            FROM OrderItem oi
            WHERE oi.productId = :productId
            """)
    BigDecimal getRevenueByProduct(@Param("productId") Long productId);

    List<OrderItem> findByBrand(String brand);

    List<OrderItem> findBySku(String sku);
}
