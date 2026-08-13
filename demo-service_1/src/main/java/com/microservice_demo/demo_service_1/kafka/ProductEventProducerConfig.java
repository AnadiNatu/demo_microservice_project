package com.microservice_demo.demo_service_1.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ProductEventProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return config;
    }

    @Bean
    public ProducerFactory<String, ProductCreatedEvent> productCreatedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseConfig());
    }

    @Bean
    public KafkaTemplate<String, ProductCreatedEvent> productCreatedKafkaTemplate() {
        return new KafkaTemplate<>(productCreatedProducerFactory());
    }

    @Bean
    public ProducerFactory<String, ProductStockUpdatedEvent> productStockUpdatedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseConfig());
    }

    @Bean
    public KafkaTemplate<String, ProductStockUpdatedEvent> productStockUpdatedKafkaTemplate() {
        return new KafkaTemplate<>(productStockUpdatedProducerFactory());
    }
}
