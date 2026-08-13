package com.microservice_demo.demo_service_1.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ProductOrderStatsCache statsCache;

    @KafkaListener(
            topics = "order-created",
            groupId = "demo-service1-group",
            containerFactory = "orderEventListenerFactory"
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("[KAFKA][DS1] Received OrderCreatedEvent | orderId={} items={}", event.getOrderId(),
                event.getItems() == null ? 0 : event.getItems().size());

        if (event.getItems() == null) return;

        for (OrderCreatedEvent.OrderedItem item : event.getItems()) {
            statsCache.increment(item.getProductId(), item.getQuantity());
        }
        log.info("[KAFKA][DS1] Order-count cache updated | orderId={}", event.getOrderId());
    }
}
