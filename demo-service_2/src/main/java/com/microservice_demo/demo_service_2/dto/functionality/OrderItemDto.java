package com.microservice_demo.demo_service_2.dto.functionality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderItemId;

    private Long productId;

    private String productName;

    private String category;

    private String brand;

    private String sku;

    private Long creatorUserId;

    private String creatorUsername;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;
}

