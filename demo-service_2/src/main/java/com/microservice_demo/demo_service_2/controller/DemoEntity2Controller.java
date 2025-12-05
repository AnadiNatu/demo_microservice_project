package com.microservice_demo.demo_service_2.controller;

import com.microservice_demo.demo_service_2.dto.AddUserListAndDE1ToDE2Dto;
import com.microservice_demo.demo_service_2.dto.AddUserToListDE1ForDE2Dto;
import com.microservice_demo.demo_service_2.dto.CreateDemoEntity2Dto;
import com.microservice_demo.demo_service_2.dto.DemoEntity2Dto;
import com.microservice_demo.demo_service_2.service.DemoEntity2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/en2")
@RequiredArgsConstructor
public class DemoEntity2Controller {

    private final DemoEntity2Service service;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public DemoEntity2Dto create(@RequestBody CreateDemoEntity2Dto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public DemoEntity2Dto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/addAll")
    @PreAuthorize("hasRole('ADMIN')")
    public DemoEntity2Dto addUsersAndDemoEntity1(@RequestBody AddUserListAndDE1ToDE2Dto dto) {
        return service.addUsersAndDemoEntity1(dto);
    }

    @PostMapping("/addUser")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public DemoEntity2Dto addUser(@RequestBody AddUserToListDE1ForDE2Dto dto) {
        return service.addUserToDemoEntity2(dto);
    }
}
