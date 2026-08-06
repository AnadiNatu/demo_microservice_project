package com.microservice_demo.demo_service_2.dto.functionality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private String sku;
    private String brand;
    private Boolean active;
    private Long createdByUserId;
    private String createdByUsername;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    private String imageUrl;
}
