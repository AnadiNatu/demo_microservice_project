package com.microservice_demo.demo_service_2.service;

import com.microservice_demo.demo_service_2.entity.Users;
import com.microservice_demo.demo_service_2.repository.UserRepository;
import com.microservice_demo.demo_service_2.dto.CreateUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    // Called by /api/users/sync endpoint when auth-service sends user sync DTO
    public Users syncUser(Long userId, String username, String email, String phone, Set<String> roles) {
        log.info("[DS2] Syncing user: userId={} email={}", userId, email);

        Optional<Users> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            log.info("[DS2] User already exists: userId={}", userId);
            Users user = existing.get();
            // Update existing user's data if needed
            user.setName(username);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(roles != null ? new HashSet<>(roles) : new HashSet<>());
            return userRepository.save(user);
        }

        // Create new user with synced data
        Users user = new Users();
        user.setUserId(userId);
        user.setName(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(roles != null ? new HashSet<>(roles) : new HashSet<>(Set.of("ROLE_USER")));
        user.setDe1ConnectionFlag(false);
        user.setDe2ConnectionFlag(true); // Mark this as synced to DS2
        user.setCreatedOn(java.time.LocalDateTime.now());
        user.setUpdatedOn(java.time.LocalDateTime.now());

        Users saved = userRepository.save(user);
        log.info("[DS2] User synced successfully: userId={}", saved.getUserId());
        return saved;
    }

    public Users getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    public void updateProfilePicture(Long userId, String profilePictureUrl) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setProfilePicture(profilePictureUrl);
        userRepository.save(user);

        log.info("[DS2] Profile picture updated | userId={}", userId);
    }
}