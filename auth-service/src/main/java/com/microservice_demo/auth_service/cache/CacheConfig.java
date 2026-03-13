package com.microservice_demo.auth_service.cache;


import com.github.benmanes.caffeine.cache.Cache;
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
    public CacheManager cacheManager(){

        log.info(" Initializiing caffeine cache manager for Auth Manager");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "users",
                "tokens"
                ,"userSync"
                ,"refreshToken"
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());
        log.info("Cache manager initialized with cache");
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder(){
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(15 , TimeUnit.MINUTES)
                .expireAfterAccess(10 , TimeUnit.MINUTES)
                .recordStats();
    }
}
