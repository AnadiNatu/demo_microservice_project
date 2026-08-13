package com.microservice_demo.demo_service_1.kafka;

public class KafkaTopics {
    private KafkaTopics() {}
    public static final String PRODUCT_CREATED = "product-created";
    public static final String PRODUCT_STOCK_UPDATED = "product-stock-updated";
    public static final String ORDER_CREATED = "order-created";
}