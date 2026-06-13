package com.microservice_demo.demo_service_1.controller;


import com.microservice_demo.demo_service_1.dto.CreateDemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.DemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.UserDto;
import com.microservice_demo.demo_service_1.entity.DemoEntity1;
import com.microservice_demo.demo_service_1.service.DemoEntity1Service;
import com.microservice_demo.demo_service_1.service.interfaces.DemoEntity1ServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/en1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class DemoEntity1Controller {

    private final DemoEntity1ServiceInterface service;

    @GetMapping("/test/service1")
    public ResponseEntity<String> publicTest(){
        return ResponseEntity.ok("Demo-Service1 is up - public endpoint OK");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DemoEntity1Dto> create(@RequestBody CreateDemoEntity1Dto dto) {
        log.info("[DemoEntity1Controller] Creating DemoEntity1 for user: {}", dto.getUserId());
        DemoEntity1Dto created = service.create(dto);
        log.info("[DemoEntity1Controller] ✅ DemoEntity1 created with ID: {}", created.getDemoEn1Id());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DemoEntity1Dto> get(@PathVariable Long id) {
        log.info("[DemoEntity1Controller] Fetching DemoEntity1 with ID: {}", id);
        DemoEntity1Dto entity = service.getEntity(id);
        log.info("[DemoEntity1Controller] ✅ DemoEntity1 found");
        return ResponseEntity.ok(entity);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<DemoEntity1Dto>> getByUser(@PathVariable Long userId) {
        log.info("[DemoEntity1Controller] Fetching DemoEntity1 list for userId: {}", userId);
        List<DemoEntity1Dto> entities = service.getDemoEntity1ByUserId(userId);
        log.info("[DemoEntity1Controller] Found {} entities for userId: {}", entities.size(), userId);
        return ResponseEntity.ok(entities);
    }

    // For Feign client calls (internal service-to-service communication)
    @GetMapping("/entity/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DemoEntity1Dto> getEntityForFeign(@PathVariable Long id) {
        log.info("[DemoEntity1Controller] Feign request - Fetching DemoEntity1 with ID: {}", id);
        DemoEntity1Dto entity = service.getDemoEntity1(id);
        log.info("[DemoEntity1Controller] ✅ Feign request - DemoEntity1 found");
        return ResponseEntity.ok(entity);
    }

//  Batch user lookup — used internally by demo-service2 Feign to resolve usernames.
    @PostMapping("/users/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<UserDto>> getUsersByIdList(@RequestBody List<Long> userIds) {
        log.info("[DemoEntity1Controller] Fetching users by ID list: {}", userIds);
        List<UserDto> users = service.getUsersByIds(userIds);
        log.info("[DemoEntity1Controller] ✅ Found {} users", users.size());
        return ResponseEntity.ok(users);
    }
}
