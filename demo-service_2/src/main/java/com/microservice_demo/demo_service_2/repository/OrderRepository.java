package com.microservice_demo.demo_service_2.repository;

import com.microservice_demo.demo_service_2.entity.Order;
import com.microservice_demo.demo_service_2.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order , Long> {

    Page<Order> findByUserId(Long userId , Pageable pageable);

    Page<Order> findByOrderStatus(OrderStatus status , Pageable pageable);

    Page<Order> findByUserIdAndOderStatus(Long userId , OrderStatus status , Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND" + "o.orderDate BETWEEN :startDate AND :endDate")
    Page<Order> findUserOrdersBetweenDates(@Param("userId") Long userId , @Param("startDate")LocalDateTime startDate , @Param("endDate")LocalDateTime endDate , Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.orderStatus IN : statuses")
    Page<Order> findByOrderStatusIn(@Param("statuses")List<OrderStatus> statuses , Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    long countByUserId(@Param("userId")Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status")
    long countByOrderStatus(@Param("status") OrderStatus status);

}
