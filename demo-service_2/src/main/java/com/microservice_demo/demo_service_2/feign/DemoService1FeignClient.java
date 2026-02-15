package com.microservice_demo.demo_service_2.feign;

import com.microservice_demo.demo_service_2.dto.DemoEntity1Dto;
import com.microservice_demo.demo_service_2.dto.UserDto;
import com.microservice_demo.demo_service_2.dto.functionality.ProductInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "demo-service1")
public interface DemoService1FeignClient {

    @GetMapping("/api/users/{id}")
    UserDto getUser(@PathVariable Long id);

    @GetMapping("/api/en1/{id}")
    DemoEntity1Dto getDemoEntity1(@PathVariable Long id);

    @GetMapping("/api/en1/user/{userId}")
    DemoEntity1Dto getDemoEntity1ByUser(@PathVariable Long userId);

    @GetMapping("/api/en1/entity/{id}")
    DemoEntity1Dto getDemoEntity1ForEn2(@PathVariable Long id);

    @PostMapping("/api/en1/users/list")
    List<UserDto> getUsersByIdList(@RequestBody List<Long> ids);

    @PostMapping("/api/products/list")
    List<ProductInfoDto> getProductsByIds(@RequestBody List<Long> productIds);

    @GetMapping("/api/products/{productId}")
    ProductInfoDto getProductById(@PathVariable("productId") Long productId);

    @PutMapping("/api/products/{productId}/stock")
    ProductInfoDto updateProductStock(@PathVariable("productId") Long productId , @RequestParam("quantity") Integer quantity);

    @GetMapping("/api/products/{productId}/available")
    Boolean isProductAvailable(@PathVariable("productId") Long productId);
}
