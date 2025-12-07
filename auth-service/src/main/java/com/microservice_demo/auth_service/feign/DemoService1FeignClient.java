package com.microservice_demo.auth_service.feign;

import com.microservice_demo.auth_service.dto.UserSyncDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "demo-service1")
public interface DemoService1FeignClient {

    @PostMapping("/api/users/sync")
    String syncUser(@RequestBody UserSyncDto syncDto);
}
