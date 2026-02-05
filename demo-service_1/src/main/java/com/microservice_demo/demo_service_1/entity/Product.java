package com.microservice_demo.demo_service_1.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products" , indexes = {
        @Index(name = "idx_product_name" , columnList = "name"),
        @Index(name = "idx_product_category" , columnList = "category"),
        @Index(name = "idx_product_created" , columnList = "createdOn")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false , length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false , precision = 10 , scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(length = 50)
    private String category;

    @Column(length = 50)
    private String sku;

    @Column(length = 50)
    private String brand;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private Users createdBy;

    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    @PrePersist
    protected void onCreate(){
        createdOn = LocalDateTime.now();
        updatedOn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedOn = LocalDateTime.now();
    }
}
