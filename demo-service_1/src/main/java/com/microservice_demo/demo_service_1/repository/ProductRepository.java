package com.microservice_demo.demo_service_1.repository;

import com.microservice_demo.demo_service_1.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByActiveTrueAndCategory(String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    Optional<Product> findBySku(String sku);

    List<Product> findByProductIdIn(List<Long> productIds);

    @Query("SELECT p FROM Product p WHERE p.createdBy.userId = :userId")
    Page<Product> findByCreatedByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    long countActiveProducts();
}
