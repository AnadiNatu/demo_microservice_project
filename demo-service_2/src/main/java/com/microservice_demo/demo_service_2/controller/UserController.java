package com.microservice_demo.demo_service_2.controller;


import com.microservice_demo.demo_service_2.dto.ProfilePictureSyncDto;
import com.microservice_demo.demo_service_2.dto.UserSyncDto;
import com.microservice_demo.demo_service_2.entity.Users;
import com.microservice_demo.demo_service_2.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(@RequestBody UserSyncDto dto) {
        log.info("[DS2] Received user sync request: email={} userId={}", dto.getEmail(), dto.getUserId());

        try {
            Users synced = userService.syncUser(
                    dto.getUserId(),
                    dto.getName() != null ? dto.getName() : dto.getEmail(),
                    dto.getEmail(),
                    dto.getPhone(),
                    dto.getRole()  // FIXED: Now accepts Set<String>
            );

            log.info("✅ [DS2] User synced successfully: userId={}", synced.getUserId());
            return ResponseEntity.ok("User synced successfully");
        } catch (Exception ex) {
            log.error("❌ [DS2] User sync failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("User sync failed: " + ex.getMessage());
        }
    }

    @PostMapping("/sync/profile-picture")
    public ResponseEntity<String> syncProfilePicture(@RequestBody ProfilePictureSyncDto dto) {
        log.info("[DS2] Syncing profile picture for userId={}", dto.getUserId());

        try {
            userService.updateProfilePicture(dto.getUserId(), dto.getProfilePictureUrl());
            log.info("✅ [DS2] Profile picture synced: userId={}", dto.getUserId());
            return ResponseEntity.ok("Profile picture synced successfully");
        } catch (Exception ex) {
            log.error("❌ [DS2] Profile picture sync failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Profile picture sync failed: " + ex.getMessage());
        }
    }

    // Keep the backward-compatible /api/en2/sync aliases
    @PostMapping("/en2/sync")
    public ResponseEntity<String> syncUserViaEn2(@RequestBody UserSyncDto dto) {
        return syncUser(dto);
    }

    @PostMapping("/en2/sync/profile-picture")
    public ResponseEntity<String> syncProfilePictureViaEn2(@RequestBody ProfilePictureSyncDto dto) {
        return syncProfilePicture(dto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Users> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }
}