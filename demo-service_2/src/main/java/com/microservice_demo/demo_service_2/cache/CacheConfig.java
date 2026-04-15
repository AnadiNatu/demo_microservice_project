package com.microservice_demo.demo_service_2.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

        @Bean
        public CacheManager cacheManager(){
            log.info("Initializing Caffeine Cache Manager");

            CaffeineCacheManager cacheManager = new CaffeineCacheManager("products" , "productPages" , "activeProducts" , "categoryProducts");

            cacheManager.setCaffeine(caffeineCacheBuilder());
            log.info("Cache Manager initialized with caches : products , productPages , activeProducts , categoryProducts");
            return cacheManager;
        }

        private Caffeine<Object , Object> caffeineCacheBuilder(){
            return Caffeine.newBuilder()
                    .initialCapacity(100)
                    .maximumSize(1000)
                    .expireAfterWrite(10 , TimeUnit.MINUTES)
                    .recordStats();
        }

//    @Bean(name = "redisObjectMapper")
//    private ObjectMapper redisObjectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        mapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//        return mapper;
//    }
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory,
//                                                       ObjectMapper redisObjectMapper) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//
//        GenericJackson2JsonRedisSerializer jsonSerializer =
//                new GenericJackson2JsonRedisSerializer(redisObjectMapper);
//
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(jsonSerializer);
//        template.setHashValueSerializer(jsonSerializer);
//        template.afterPropertiesSet();
//
//        log.info("[DS2 Cache] RedisTemplate configured");
//        return template;
//    }
//
//    @Primary
//    @Bean
//    public CacheManager cacheManager(RedisConnectionFactory factory) {
//        ObjectMapper redisObjectMapper = redisObjectMapper();
//
//        GenericJackson2JsonRedisSerializer jsonSerializer =
//                new GenericJackson2JsonRedisSerializer(redisObjectMapper);
//
//        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(5))
//                .disableCachingNullValues()
//                .serializeKeysWith(
//                        RedisSerializationContext.SerializationPair
//                                .fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(
//                        RedisSerializationContext.SerializationPair
//                                .fromSerializer(jsonSerializer));
//
//        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
//        perCache.put("orders",       base.entryTtl(Duration.ofMinutes(5)));
//        perCache.put("userOrders",   base.entryTtl(Duration.ofMinutes(5)));
//        perCache.put("statusOrders", base.entryTtl(Duration.ofMinutes(5)));
//
//        log.info("[DS2 Cache] RedisCacheManager initialised with {} named caches", perCache.size());
//
//        return RedisCacheManager.builder(factory)
//                .cacheDefaults(base)
//                .withInitialCacheConfigurations(perCache)
//                .transactionAware()
//                .build();
//    }
}