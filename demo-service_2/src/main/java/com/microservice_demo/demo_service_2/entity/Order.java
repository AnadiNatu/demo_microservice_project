package com.microservice_demo.demo_service_2.entity;

import com.microservice_demo.demo_service_2.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders" , indexes = {
        @Index(name = "idx_order_number" , columnList = "orderNumber"),
        @Index(name = "idx_order_status" , columnList = "createdOn")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false , precision = 10 , scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(length = 500)
    private String shippingAddress;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Long userId;

    @ElementCollection
    @CollectionTable(
            name = "order_product_items",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @Column(name = "product_id")
    private List<Long> productIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "order_product_quantities" , joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private List<Integer> quantities = new ArrayList<>();

    private LocalDateTime createdOn;

    private LocalDateTime updatedOn;

    private LocalDateTime deliveryDate;

    private LocalDateTime orderDate;

    @PrePersist
    protected void onCreate(){
        createdOn = LocalDateTime.now();
        updatedOn = LocalDateTime.now();
        if(orderNumber == null){
            orderNumber = generateOrderNumber();
        }
    }

    @PreUpdate
    protected void onUpdate(){
        updatedOn = LocalDateTime.now();
    }

    private String generateOrderNumber(){
        return "ORD-" + System.currentTimeMillis();
    }


}
