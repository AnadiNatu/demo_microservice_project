package com.microservice_demo.demo_service_2.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing Caffeine Cache Manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "orders",
                "userOrders",
                "statusOrders"
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());

        log.info("Cache Manager initialized with caches: orders, userOrders, statusOrders");
        return cacheManager;
    }

    /**
     * Configure Caffeine cache builder
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }
}