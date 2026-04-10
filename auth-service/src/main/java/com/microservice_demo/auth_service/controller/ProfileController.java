package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.repository.UserRepository;
import com.microservice_demo.auth_service.security.UserDetailsServiceImpl;
import com.microservice_demo.auth_service.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile/")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final CloudinaryService cloudinaryService;
    private final UserRepository userType1Repository;

    @PostMapping("photo")
    public ResponseEntity<Map<String, Object>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        log.info("[PROFILE] Photo upload request | email={}", email);

        if (userDetails instanceof UserDetailsServiceImpl d) {
            var entity = userType1Repository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String url = cloudinaryService.uploadProfilePhoto(file, "t1_" + entity.getId());
            entity.setProfilePicture(url);
            userType1Repository.save(entity);

            log.info("[PROFILE] TYPE1 photo updated | id={} | url={}", entity.getId(), url);
            return ResponseEntity.ok(Map.of(
                    "message",    "Profile photo updated successfully",
                    "photoUrl",   url,
                    "userType",   "TYPE1"
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Unknown user type"));
    }

    @GetMapping("photo")
    public ResponseEntity<Map<String, Object>> getProfilePhoto(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();

        if (userDetails instanceof UserDetailsServiceImpl d) {
            var entity = userType1Repository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(Map.of(
                    "photoUrl", entity.getProfilePicture() != null ? entity.getProfilePicture() : "",
                    "userType", "TYPE1"
            ));

        }
        return ResponseEntity.badRequest().body(Map.of("error", "Unknown user type"));
    }

    @DeleteMapping("photo")
    public ResponseEntity<Map<String, Object>> removeProfilePhoto(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();

        if (userDetails instanceof UserDetailsServiceImpl d) {
            var entity = userType1Repository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (entity.getProfilePicture() != null) {
                cloudinaryService.deleteImage("multiuser/profiles/user_t1_" + entity.getId());
                entity.setProfilePicture(null);
                userType1Repository.save(entity);
            }
            return ResponseEntity.ok(Map.of("message", "Profile photo removed"));

        }
        return ResponseEntity.badRequest().body(Map.of("error", "Unknown user type"));
    }
}

