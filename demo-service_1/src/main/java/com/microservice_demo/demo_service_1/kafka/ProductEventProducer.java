package com.microservice_demo.demo_service_1.kafka;

import com.microservice_demo.demo_service_1.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventProducer {

    private final KafkaTemplate<String, ProductCreatedEvent> productCreatedKafkaTemplate;
    private final KafkaTemplate<String, ProductStockUpdatedEvent> productStockUpdatedKafkaTemplate;

    // KAFKA: Publish ProductCreatedEvent after successful product creation.
    public void publishProductCreated(Product product) {
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .sku(product.getSku())
                .brand(product.getBrand())
                .createdByUserId(product.getCreatedBy() != null ? product.getCreatedBy().getUserId() : null)
                .build();

        productCreatedKafkaTemplate.send(KafkaTopics.PRODUCT_CREATED, String.valueOf(product.getProductId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to publish ProductCreatedEvent | productId={} | error={}", product.getProductId(), ex.getMessage());
                    } else {
                        log.info("[KAFKA] ProductCreatedEvent published | productId={}", product.getProductId());
                    }
                });
    }

    // KAFKA: Publish ProductStockUpdatedEvent after a stock change.
    public void publishProductStockUpdated(Long productId, Integer previousStock, Integer newStock) {
        ProductStockUpdatedEvent event = ProductStockUpdatedEvent.builder()
                .productId(productId)
                .previousStockQuantity(previousStock)
                .newStockQuantity(newStock)
                .build();

        productStockUpdatedKafkaTemplate.send(KafkaTopics.PRODUCT_STOCK_UPDATED, String.valueOf(productId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to publish ProductStockUpdatedEvent | productId={} | error={}", productId, ex.getMessage());
                    } else {
                        log.info("[KAFKA] ProductStockUpdatedEvent published | productId={} | {} -> {}", productId, previousStock, newStock);
                    }
                });
    }

}
