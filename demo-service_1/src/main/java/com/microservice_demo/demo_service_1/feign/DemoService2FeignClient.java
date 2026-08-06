package com.microservice_demo.demo_service_1.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "demo-service2", contextId = "demoService2OrderClient")
public interface DemoService2FeignClient {

    @GetMapping("/api/orders/product/{productId}/count")
    Long getProductOrderCount(@PathVariable("productId") Long productId);

    @GetMapping("/api/orders/user/{userId}/exists")
    Boolean userHasOrders(@PathVariable("userId") Long userId);
}