package com.microservice_demo.demo_service_1.controller;


import com.microservice_demo.demo_service_1.dto.CreateDemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.DemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.UserDto;
import com.microservice_demo.demo_service_1.entity.DemoEntity1;
import com.microservice_demo.demo_service_1.service.DemoEntity1Service;
import com.microservice_demo.demo_service_1.service.interfaces.DemoEntity1ServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/en1")
@RequiredArgsConstructor
public class DemoEntity1Controller {

    private final DemoEntity1ServiceInterface service;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public DemoEntity1Dto create(@RequestBody CreateDemoEntity1Dto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public DemoEntity1Dto get(@PathVariable Long id) {
        return service.getEntity(id);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public DemoEntity1Dto getByUser(@PathVariable Long userId) {
        return service.getDemoEntity1ByUserId(userId);
    }

    // For Feign client calls (internal service-to-service communication)
    @GetMapping("/entity/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public DemoEntity1Dto getEntityForFeign(@PathVariable Long id) {
        return service.getDemoEntity1(id);
    }

    @PostMapping("/users/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<UserDto> getUsersByIdList(@RequestBody List<Long> userIds) {
        return service.getUsersByIds(userIds);
    }
}
