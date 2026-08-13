package com.microservice_demo.demo_service_1.kafka;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class ProductOrderStatsCache {
    private final ConcurrentHashMap<Long, LongAdder> orderCounts = new ConcurrentHashMap<>();

    public void increment(Long productId, int quantity) {
        orderCounts.computeIfAbsent(productId, id -> new LongAdder()).add(quantity);
    }

    public Long getCount(Long productId) {
        LongAdder adder = orderCounts.get(productId);
        return adder == null ? 0L : adder.sum();
    }
}
