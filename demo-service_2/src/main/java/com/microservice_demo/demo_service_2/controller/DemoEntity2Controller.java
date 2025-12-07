package com.microservice_demo.demo_service_2.controller;

import com.microservice_demo.demo_service_2.dto.*;
import com.microservice_demo.demo_service_2.entity.Users;
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

    @PostMapping("/sync")
    public String syncUser(@RequestBody UserSyncDto syncDto){
        CreateUserDto dto = new CreateUserDto();
        dto.setName(syncDto.getUsername());
        dto.setEmail(syncDto.getEmail());
        dto.setPhone("");

        String role = syncDto.getRoles().isEmpty() ? "USER" : syncDto.getRoles().iterator().next();

        dto.setUserRole(role);
        service.createUser(dto);

        return "User synced successfully";
    }

    @GetMapping("/user/{id}")
    @PreAuthorize("hasAnyRole('USER' , 'ADMIN')")
    public Users getUsers(@PathVariable Long id){
        return service.getUser(id);
    }
}
