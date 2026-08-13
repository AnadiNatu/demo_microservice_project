package com.microservice_demo.demo_service_2.kafka;

import com.microservice_demo.demo_service_2.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderCreatedEvent> orderCreatedKafkaTemplate;

    // KAFKA: Publish OrderCreatedEvent after the order is persisted and stock has been decremented in DS1.
    public void publishOrderCreated(Order order) {
        List<OrderCreatedEvent.OrderedItem> items = order.getItems() == null ? List.of() :
                order.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderedItem.builder()
                             .productId(item.getProductId())
                             .quantity(item.getQuantity())
                             .build())
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .items(items)
                .build();

        orderCreatedKafkaTemplate.send(KafkaTopics.ORDER_CREATED, String.valueOf(order.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to publish OrderCreatedEvent | orderId={} | error={}", order.getOrderId(), ex.getMessage());
                    } else {
                        log.info("[KAFKA] OrderCreatedEvent published | orderId={}", order.getOrderId());
                    }
                });
    }
}