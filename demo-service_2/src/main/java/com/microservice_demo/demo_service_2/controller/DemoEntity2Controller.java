package com.microservice_demo.demo_service_2.controller;

import com.microservice_demo.demo_service_2.dto.*;
import com.microservice_demo.demo_service_2.entity.Users;
import com.microservice_demo.demo_service_2.service.DemoEntity2Service;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/en2")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DemoEntity2Controller {

    private final DemoEntity2Service service;

//    Public smoke-test
    @GetMapping("/test/service2")
    public ResponseEntity<String> publicTest() {
        return ResponseEntity.ok("Demo-Service2 is up — public endpoint OK");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DemoEntity2Dto> create(@RequestBody CreateDemoEntity2Dto dto) {
        log.info("[DS2 En2] Create");
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DemoEntity2Dto> get(@PathVariable Long id) {
        log.info("[DS2 En2] Fetch | id={}", id);
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/addAll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DemoEntity2Dto> addUsersAndDemoEntity1(@RequestBody AddUserListAndDE1ToDE2Dto dto) {
        log.info("[DS2 En2] addAll | de2Id={}", dto.getDemoEn2Id());
        return ResponseEntity.ok(service.addUsersAndDemoEntity1(dto));
    }

    @PostMapping("/addUser")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<DemoEntity2Dto> addUser(@RequestBody AddUserToListDE1ForDE2Dto dto) {
        return ResponseEntity.ok(service.addUserToDemoEntity2(dto));
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(@RequestBody UserSyncDto syncDto){
        log.info("[DS2] Received user sync | email={} id={}", syncDto.getEmail(), syncDto.getId());

        CreateUserDto dto = new CreateUserDto();
        dto.setName(syncDto.getUsername());
        dto.setEmail(syncDto.getEmail());
        dto.setPhone(syncDto.getPhoneNumber() != null ? syncDto.getPhoneNumber() : "");

        String role = "USER";
        if (syncDto.getRoles() != null && !syncDto.getRoles().isEmpty()) {
            String raw = syncDto.getRoles().iterator().next();
            role = raw.replace("ROLE_", "").toUpperCase();
        }
        dto.setUserRole(role);

        // BUG FIX: pass auth-service ID so row uses the same PK
        Users createdUser = service.createUser(dto, syncDto.getId());

        if (syncDto.getProfilePicture() != null && !syncDto.getProfilePicture().isBlank()) {
            service.updateProfilePicture(createdUser.getUserId(), syncDto.getProfilePicture());
        }

        log.info("[DS2] User synced | email={}", createdUser.getEmail());
        return ResponseEntity.ok("User synced successfully to Demo-Service2");
    }

//    @PreAuthorize("hasAnyRole('USER' , 'ADMIN')")
    @GetMapping("/user/{id}")
    public Users getUsers(@PathVariable Long id){
        return service.getUser(id);
    }

    @PostMapping("/sync/profile-picture")
    public String syncProfilePicture(@RequestBody ProfilePictureSyncDto syncDto) {
        log.info("[DS2] Received profile picture sync | userId={}", syncDto.getUserId());

        service.updateProfilePicture(syncDto.getUserId(), syncDto.getProfilePictureUrl());

        log.info("[DS2] Profile picture synced | userId={}", syncDto.getUserId());
        return "Profile picture synced successfully";
    }
}
