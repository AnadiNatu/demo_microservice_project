package com.microservice_demo.demo_service_2.kafka;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private List<OrderedItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderedItem implements Serializable {
        private Long productId;
        private Integer quantity;
    }
}
