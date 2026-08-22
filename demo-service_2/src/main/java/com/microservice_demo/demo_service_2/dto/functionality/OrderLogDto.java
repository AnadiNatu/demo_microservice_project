package com.microservice_demo.demo_service_2.dto.functionality;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogDto {

    private Long orderId;

    private String productName;

    private String userName;

    private Integer orderQuantity;

    private BigDecimal orderPrice;

    private String orderStatus;

    private LocalDateTime deliveredOn;

    private Integer productInventory;

    private Integer productOrderQuantity;

}