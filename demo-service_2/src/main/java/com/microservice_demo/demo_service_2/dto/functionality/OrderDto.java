package com.microservice_demo.demo_service_2.dto.functionality;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String username;
    private List<Long> productIds;
    private List<ProductInfoDto> productDetails;
    private List<Integer> quantities;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String shippingAddress;
    private String notes;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}