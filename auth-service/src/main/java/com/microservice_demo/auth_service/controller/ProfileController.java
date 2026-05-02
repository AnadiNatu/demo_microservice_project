package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.repository.UserRepository;
import com.microservice_demo.auth_service.security.UserDetailsServiceImpl;
import com.microservice_demo.auth_service.service.AuthService;
import com.microservice_demo.auth_service.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
    private final UserRepository userRepository;
    private final AuthService authService;

    @PostMapping(value = "photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        log.info("[PROFILE] Photo upload request | email={}", email);

        Users user = userRepository.findByUsername(email).orElseThrow(() -> new RuntimeException("User not found"));

        String url = cloudinaryService.uploadProfilePhoto(file , "user_"+user.getId());
        user.setProfilePicture(url);
        userRepository.save(user);

        authService.syncProfilePictureUpdate(user.getId(), url);

        return ResponseEntity.ok(Map.of(
                "message", "Profile photo updated successfully",
                "photoUrl", url,
                "synced", true
        ));
    }

    @GetMapping("photo")
    public ResponseEntity<Map<String, Object>> getProfilePhoto(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        Users user = requireUser(email);

//       Users user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

       return ResponseEntity.ok(Map.of(
               "photoUrl", user.getProfilePicture() != null ? user.getProfilePicture() : "",
               "userId", user.getId()
       ));
    }

    @DeleteMapping("photo")
    public ResponseEntity<Map<String, Object>> removeProfilePhoto(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();

        Users user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProfilePicture() != null) {
            String publicId = cloudinaryService.extractPublicId(user.getProfilePicture());

            if (publicId != null) {
                cloudinaryService.deleteImage(publicId);
            }
            user.setProfilePicture(null);
            userRepository.save(user);

            authService.syncProfilePictureUpdate(user.getId(), null);

            log.info("[PROFILE] Photo removed and synced | userId={}", user.getId());
        }

        return ResponseEntity.ok(Map.of(
                "message", "Profile photo removed",
                "synced", true
        ));
    }

    @PutMapping("me")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        Users user = requireUser(email);

        String newUsername = body.get("username");
        String newPhone    = body.get("phoneNumber");

        if (newUsername != null && !newUsername.isBlank()) {
            // Prevent duplicate usernames
            if (userRepository.existsByUsername(newUsername)
                    && !newUsername.equals(user.getUsername())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Username '" + newUsername + "' is already taken"
                ));
            }
            user.setUsername(newUsername);
        }

        if (newPhone != null && !newPhone.isBlank()) {
            user.setPhoneNumber(newPhone);
        }

        userRepository.save(user);
        log.info("[PROFILE] Profile updated | userId={}", user.getId());

        return ResponseEntity.ok(Map.of(
                "message",     "Profile updated successfully",
                "username",    user.getUsername(),
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
        ));
    }

@GetMapping("me")
public ResponseEntity<Map<String , Object>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails){

        String username = userDetails.getUsername();
        Users user = requireUser(username);

        return ResponseEntity.ok(Map.of(
                "id" , user.getId(),
                "username" , user.getUsername(),
                "email" , user.getEmail(),
                "phoneNumber" , user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                "profilePicture" , user.getProfilePicture() != null ? user.getProfilePicture() : "",
                "roles" , user.getRoles(),
                "provider" , user.getProvider() != null ? user.getProvider() : "LOCAL"
        ));
}

//    Private helper
private Users requireUser(String username) {
    return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
}
}

