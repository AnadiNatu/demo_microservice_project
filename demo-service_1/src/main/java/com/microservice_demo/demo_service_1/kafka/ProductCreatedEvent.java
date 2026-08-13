package com.microservice_demo.demo_service_1.kafka;


import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent implements Serializable {

    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private String sku;
    private String brand;
    private Long createdByUserId;

}