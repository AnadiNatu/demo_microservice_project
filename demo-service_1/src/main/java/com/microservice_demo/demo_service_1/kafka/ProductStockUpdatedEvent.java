package com.microservice_demo.demo_service_1.kafka;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockUpdatedEvent implements Serializable{
    private Long productId;
    private Integer previousStockQuantity;
    private Integer newStockQuantity;
}
