package com.microservice_demo.demo_service_2.dto.functionality;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;

    private String orderNumber;

    private Long userId;

    private String username;

    private String orderStatus;

    private BigDecimal totalAmount;

    private List<OrderItemDto> items;

    private String shippingName;

    private String shippingPhone;

    private String shippingEmail;

    private String shippingAddress;

    private String shippingCity;

    private String shippingState;

    private String shippingCountry;

    private String postalCode;

    private String notes;

    private LocalDateTime orderDate;

    private LocalDateTime createdOn;

    private LocalDateTime updatedOn;

    private LocalDateTime shippedDate;

    private LocalDateTime estimatedDelivery;

    private LocalDateTime deliveryDate;

    private LocalDateTime cancelledDate;
}