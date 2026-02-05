package com.microservice_demo.demo_service_2.dto.functionality;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInfoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
}