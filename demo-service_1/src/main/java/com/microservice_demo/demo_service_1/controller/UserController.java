package com.microservice_demo.demo_service_1.controller;
import com.microservice_demo.demo_service_1.dto.CreateUserDto;
import com.microservice_demo.demo_service_1.dto.UserSyncDto;
import com.microservice_demo.demo_service_1.dto.functionality.ProfilePictureSyncDto;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService service;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
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
    public ResponseEntity<String> syncUser(@RequestBody UserSyncDto dto) {
        log.info("[DS1] Received sync request: userId={} email={}", dto.getUserId(), dto.getEmail());

        try {
            // Extract userId from the DTO
            Long userId = dto.getUserId();
            if (userId == null) {
                log.error("[DS1] Missing userId in sync request");
                return ResponseEntity.badRequest().body("userId is required");
            }

            // Create the user with the userId from auth-service
            CreateUserDto createDto = CreateUserDto.builder()
                    .name(dto.getUsername())
                    .email(dto.getEmail())
                    .phone(dto.getPhone())
                    .userRole(dto.getRole() != null && !dto.getRole().isEmpty()
                            ? dto.getRole().iterator().next()
                            : "ROLE_USER")
                    .build();

            Users synced = service.createUser(createDto, userId);

            log.info("✅ [DS1] User synced successfully: userId={}", synced.getUserId());
            return ResponseEntity.ok("User synced successfully");

        } catch (Exception ex) {
            log.error("❌ [DS1] Sync failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Sync failed: " + ex.getMessage());
        }
    }

//    @PostMapping("/sync")
//    public ResponseEntity<String> syncUser(@RequestBody UserSyncDto syncDto) {
//        log.info("[UserController] Received user sync for: {}", syncDto.getEmail());
//
//        CreateUserDto dto = new CreateUserDto();
//        dto.setName(syncDto.getUsername());
//        dto.setEmail(syncDto.getEmail());
////        dto.setPhone(syncDto.getPhoneNumber() != null ? syncDto.getPhoneNumber() : "");
//
//        String role = "ROLE_USER";
//        if (syncDto.getRole() != null && !syncDto.getRole().isEmpty()) {
//            String raw = syncDto.getRole().iterator().next();
//            role = raw.startsWith("ROLE_") ? raw : "ROLE_" + raw.toUpperCase();
//        }
//        dto.setUserRole(role);
//
//        // CRITICAL: pass the auth-service ID so the local row is stored with the same PK
//        Users saved = service.createUser(dto, syncDto.getUserId());
//
//        // Sync profile picture if available
////        if (syncDto.getProfilePicture() != null && !syncDto.getProfilePicture().isBlank()) {
////            service.updateProfilePicture(saved.getUserId(), syncDto.getProfilePicture());
////        }
//
//        log.info("[DS1 Sync] User upserted | id={} email={}", saved.getUserId(), saved.getEmail());
//        return ResponseEntity.ok("User synced successfully to Demo-Service1");
//    }

    @PostMapping("/sync/profile-picture")
    public ResponseEntity<String> syncProfilePicture(@RequestBody ProfilePictureSyncDto syncDto) {
        log.info("[DS1 Sync] Profile-picture sync | userId={}", syncDto.getUserId());
        service.updateProfilePicture(syncDto.getUserId(), syncDto.getProfilePictureUrl());
        log.info("[DS1 Sync] Profile-picture synced | userId={}", syncDto.getUserId());
        return ResponseEntity.ok("Profile picture synced successfully");
    }

//    Local photo upload

    @PostMapping("/{id}/uploadLocal")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> uploadLocal(@PathVariable Long id, @RequestParam MultipartFile file) {
        log.info("[UserController] Uploading photo for user ID: {}", id);
        String result = service.uploadPhotoToFolder(id, file);
        log.info("[UserController]Photo uploaded: {}", result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/photoLocal")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<byte[]> getLocalPhoto(@PathVariable Long id) {
        log.info("[UserController] Fetching photo for user ID: {}", id);
        byte[] photo = service.getProfilePhotoFromFolder(id);
        log.info("[UserController]Photo retrieved successfully");
        return ResponseEntity.ok(photo);
    }
}