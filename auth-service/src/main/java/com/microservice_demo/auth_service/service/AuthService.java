package com.microservice_demo.auth_service.service;

import com.microservice_demo.auth_service.dto.*;
import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.feign.DemoService1FeignClient;
import com.microservice_demo.auth_service.feign.DemoService2FeignClient;
import com.microservice_demo.auth_service.notifcation.NotificationService;
import com.microservice_demo.auth_service.repository.UserRepository;
import com.microservice_demo.auth_service.security.JwtTokenProvider;
import com.microservice_demo.auth_service.security.UserDetailsServiceImpl;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;


import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final NotificationService notificationService;
    private final UserSyncService userSyncService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users" , allEntries = true),
            @CacheEvict(value = "userSync" , allEntries = true)
    })
    public AuthResponse register(RegisterRequest request) {
        log.info("[AUTH - SERVICE] Registration request received for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("Registration failed: Username '{}' is already taken", request.getUsername());
            throw new RuntimeException("Error : Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("Registration failed: Email '{}' is already in use", request.getEmail());
            throw new RuntimeException("Error : Email is already in use");
        }

        Set<String> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            roles.add("ROLE_USER");
            log.debug("No roles provided, assigning default: ROLE_USER");
        } else {
            request.getRoles().forEach(role -> {
                String processedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                roles.add(processedRole);
                log.debug("Added role: {}", processedRole);
            });
        }

        Users user = Users.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        Users savedUser = userRepository.save(user);
        log.info("User created successfully : {} with roles : {}" ,savedUser.getUsername() , roles);

        syncUserToMicroservices(user);

//        Auto-login after registration
        log.debug("Performing auto-login for user: {}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        Users userDetails = (Users) authentication.getPrincipal();

        log.info("Registration completed successfully for user: {}", userDetails.getUsername());
        return AuthResponse.builder()
                .id(userDetails.getId())
                .token(jwt)
                .refreshToken(refreshToken)
                .username(userDetails.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for identifier: {}", request.getUsername());

        // 1. Resolve identifier → Users entity
        String identifier = request.getUsername().trim();
        Optional<Users> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhoneNumber(identifier);
        }

        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        Users resolvedUser = userOpt.get();

        // 2. If this is a pure OAuth2 account (no local password), issue token directly
        if (resolvedUser.getPassword() == null || resolvedUser.getPassword().isBlank()) {
            log.info("OAuth2-only account — issuing token directly | email={}", resolvedUser.getEmail());
            String jwt = jwtTokenProvider.generateTokenFromUser(resolvedUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(resolvedUser.getUsername());

            return AuthResponse.builder()
                    .id(resolvedUser.getId())
                    .token(jwt)
                    .refreshToken(refreshToken)
                    .username(resolvedUser.getUsername())
                    .email(resolvedUser.getEmail())
                    .roles(resolvedUser.getRoles())
                    .expiresIn(jwtTokenProvider.getExpirationMs())
                    .build();
        }

        // 3. Standard password authentication (using the resolved username so
        //    DaoAuthenticationProvider can load the user correctly)
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            resolvedUser.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(resolvedUser.getUsername());
            Users users = (Users) authentication.getPrincipal();

            if (users.getPhoneNumber() != null && !users.getPhoneNumber().isBlank()) {
                try {
                    notificationService.sendLoginAlert(users.getPhoneNumber(), users.getUsername());
                } catch (Exception ex) {
                    log.warn("Login alert SMS failed: {}", ex.getMessage());
                }
            }

            log.info("Login successful for user: {}", users.getUsername());
            return AuthResponse.builder()
                    .id(users.getId())
                    .token(jwt)
                    .refreshToken(refreshToken)
                    .username(users.getUsername())
                    .email(users.getEmail())
                    .roles(users.getRoles())
                    .expiresIn(jwtTokenProvider.getExpirationMs())
                    .build();

        } catch (Exception ex) {
            log.error("Login failed for identifier: {} — {}", identifier, ex.getMessage());
            throw ex;
        }
    }

    @Cacheable(value = "refreshToken" , key = "#request.refreshToken")
    public AuthResponse refreshToken(RefreshTokenRequest request){
        log.info("Token refresh request received");
        String refreshToken = request.getRefreshToken();

        // Reject (invalid/expired) tokens immediately
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.error("Invalid or expired refresh token provided");
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        log.debug("Refreshing token for user: {}", username);

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Users userDetails = Users.build(users);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newJwt = jwtTokenProvider.generateToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        log.info("Token refreshed successfully for user: {}", username);
        return AuthResponse.builder()
                .id(users.getId())
                .token(newJwt)
                .refreshToken(newRefreshToken)
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(users.getRoles())
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();
    }

    @Cacheable(value = "token" , key = "#request.token")
    public ValidateTokenResponse validateToken(ValidateTokenRequest request) {
        log.debug("Token validation request received");
        try {
            String token = request.getToken();
            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                Set<String> roles = jwtTokenProvider.getRolesFromToken(token);

                log.info("Token is valid for user: {} with roles: {}", username, roles);

                return ValidateTokenResponse.builder()
                        .valid(true)
                        .username(username)
                        .roles(roles)
                        .message("Token is valid")
                        .build();
            } else {
                log.warn("Invalid token provided");
                return ValidateTokenResponse.builder()
                        .valid(false)
                        .message("Token is inalid")
                        .build();
            }
        } catch (Exception ex) {
            log.error("Token validation failed: {}", ex.getMessage());
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + ex.getMessage())
                    .build();
        }
    }

    public UserSyncDto getUserSyncData(Long userId){
        log.debug("Fetching user sync data for userId: {}", userId);
        Users user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        log.debug("User sync data retrieved for: {}", user.getUsername());

        return UserSyncDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .profilePicture(user.getProfilePicture())
                .build();
    }

    @CircuitBreaker(name = "microserviceSync", fallbackMethod = "syncUserFallback")
    @Retry(name = "microserviceSync")
    public void syncUserToMicroservices(Users user) {
        log.info("[SYNC] Syncing user to microservices | id={} username={}", user.getId(), user.getUsername());

        UserSyncDto syncDto = UserSyncDto.builder()
                .id(user.getId())                   // ← auth-service PK — must be preserved downstream
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .profilePicture(user.getProfilePicture())
                .build();

        // Demo-Service-1
        try {
            userSyncService.syncUserToMicroservices(user);
            log.info("[SYNC] User synced to Demo-Service1 | id={}", user.getId());
        } catch (Exception ex) {
            log.error("[SYNC] Demo-Service1 sync failed | id={} | error={}", user.getId(), ex.getMessage());
        }

        // Demo-Service-2
        try {
            userSyncService.syncUserToMicroservices(user);
            log.info("[SYNC] User synced to Demo-Service2 | id={}", user.getId());
        } catch (Exception ex) {
            log.error("[SYNC] Demo-Service2 sync failed | id={} | error={}", user.getId(), ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void syncUserFallback(Users user, Exception e) {
        log.warn("Circuit breaker activated for user sync - User: {} - Error: {}",
                user.getUsername(), e.getMessage());
        log.info("User registration will proceed without sync. Manual sync may be required.");
    }

//    Profile photo sync to all microservice
public void syncProfilePictureUpdate(Long userId, String profilePictureUrl) {
    log.info("[SYNC] Syncing profile-picture | userId={}", userId);
    try {
        ProfilePictureSyncDto syncDto = ProfilePictureSyncDto.builder()
                .userId(userId)
                .profilePictureUrl(profilePictureUrl)
                .build();
        userSyncService.syncProfilePictureUpdate(userId , profilePictureUrl);
        log.info("[SYNC] Profile-picture synced | userId={}", userId);
    } catch (Exception ex) {
        log.error("[SYNC] Profile-picture sync failed | userId={} | error={}", userId, ex.getMessage());
    }
}
}
