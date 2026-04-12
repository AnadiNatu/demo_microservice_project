package com.microservice_demo.demo_service_1.controller;
import com.microservice_demo.demo_service_1.dto.CreateUserDto;
import com.microservice_demo.demo_service_1.dto.UserSyncDto;
import com.microservice_demo.demo_service_1.dto.functionality.ProfilePictureSyncDto;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class UserController {

    private final UserService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Users> create(@RequestBody CreateUserDto dto) {
        log.info("[UserController] Creating user: {}", dto.getEmail());
        Users created = service.createUser(dto);
        log.info("[UserController] ✅ User created successfully: {}", created.getEmail());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Users> get(@PathVariable Long id) {
        log.info("[UserController] Fetching user with ID: {}", id);
        Users user = service.getUser(id);
        log.info("[UserController] ✅ User found: {}", user.getEmail());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(@RequestBody UserSyncDto syncDto) {
        log.info("[UserController] 📥 Received user sync request for: {}", syncDto.getEmail());

        CreateUserDto dto = new CreateUserDto();
        dto.setName(syncDto.getUsername());
        dto.setEmail(syncDto.getEmail());
        dto.setPhone("");

        // Convert Set<String> roles to single role string
        String role = "USER";
        if (syncDto.getRoles() != null && !syncDto.getRoles().isEmpty()) {
            role = syncDto.getRoles().iterator().next().replace("ROLE_", "");
        }
        dto.setUserRole(role);

        Users createdUser = service.createUser(dto);

        if (syncDto.getProfilePicture() != null && !syncDto.getProfilePicture().isBlank()){
            service.updateProfilePicture(createdUser.getUserId() , syncDto.getProfilePicture());
        }
        log.info("[UserController] ✅ User synced successfully: {}", createdUser.getEmail());

        return ResponseEntity.ok("User synced successfully to Demo-Service1");
    }

    @PostMapping("/{id}/uploadLocal")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> uploadLocal(@PathVariable Long id, @RequestParam MultipartFile file) {
        log.info("[UserController] Uploading photo for user ID: {}", id);
        String result = service.uploadPhotoToFolder(id, file);
        log.info("[UserController] ✅ Photo uploaded: {}", result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/profile-picture")
    public String syncProfilePicture(@RequestBody ProfilePictureSyncDto syncDto) {
        log.info("📥 [DS1] Received profile picture sync | userId={}", syncDto.getUserId());

        service.updateProfilePicture(syncDto.getUserId(), syncDto.getProfilePictureUrl());

        log.info("✅ [DS1] Profile picture synced | userId={}", syncDto.getUserId());
        return "Profile picture synced successfully";
    }

    @GetMapping("/{id}/photoLocal")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<byte[]> getLocalPhoto(@PathVariable Long id) {
        log.info("[UserController] Fetching photo for user ID: {}", id);
        byte[] photo = service.getProfilePhotoFromFolder(id);
        log.info("[UserController] ✅ Photo retrieved successfully");
        return ResponseEntity.ok(photo);
    }
}
