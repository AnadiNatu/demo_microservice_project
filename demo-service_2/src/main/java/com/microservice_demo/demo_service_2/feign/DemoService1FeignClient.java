package com.microservice_demo.demo_service_2.feign;

import com.microservice_demo.demo_service_2.dto.DemoEntity1Dto;
import com.microservice_demo.demo_service_2.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
