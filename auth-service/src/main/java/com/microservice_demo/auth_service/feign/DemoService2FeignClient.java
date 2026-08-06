package com.microservice_demo.auth_service.feign;

import com.microservice_demo.auth_service.dto.UserSyncDto;
import com.microservice_demo.auth_service.dto.ProfilePictureSyncDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "demo-service2", contextId = "demoService2SyncClient")
public interface DemoService2FeignClient {

    // FIXED: Changed /api/en2/sync → /api/users/sync for consistency with demo-service1
    @PostMapping("/api/users/sync")
    String syncUser(@RequestBody UserSyncDto syncDto);

    @PostMapping("/api/users/sync/profile-picture")
    String syncProfilePicture(@RequestBody ProfilePictureSyncDto syncDto);
}