package com.microservice_demo.demo_service_1.dto.functionality;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductInfoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;

    private String productName;

    private String description;

    private BigDecimal price;

    private Integer stockQuantity;

    private String category;

    private String sku;

    private String brand;

    private String imageUrl;

    private Boolean active;

    private Long creatorUserId;

    private String creatorUsername;

}