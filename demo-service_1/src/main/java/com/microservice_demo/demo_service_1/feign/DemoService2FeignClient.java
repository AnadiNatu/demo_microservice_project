package com.microservice_demo.demo_service_1.feign;

import com.microservice_demo.demo_service_1.dto.UserSyncDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "demo-service2")
public interface DemoService2FeignClient {

    @PostMapping("/api/en2/sync")
    String syncUser(@RequestBody UserSyncDto syncDto);

    @GetMapping("/api/orders/product/{productId}/count")
    Long getProductOrderCount(@PathVariable("productId") Long productId);

    @GetMapping("/api/orders/user/{userId}/exists")
    Boolean userHasOrders(@PathVariable("userId") Long userId);
}