package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.security.JwtTokenProvider;
import com.microservice_demo.auth_service.service.OAuth2ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth2/")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2ServiceImpl oAuth2Service;

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> oauth2Success(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            log.error("[OAUTH2] No OAuth2User principal found");
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Authentication failed",
                    "message", "No user information received from OAuth2 provider"
            ));
        }

        try {
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String provider = oAuth2Service.determineProvider(oAuth2User);

            // Get provider ID
            String providerId = oAuth2User.getAttribute("sub");
            if (providerId == null) {
                Object idObj = oAuth2User.getAttribute("sub");
                providerId = idObj != null ? idObj.toString() : null;
            }

            log.info("[OAUTH2] Login attempt | provider={} | email={}", provider, email);

            // Handle OAuth user (create or update)
//            Users user = oAuth2Service.handleOAuthUser(email, name, provider, providerId);

            // Creates or updates the user, and syncs profile picture from provider
            Users user = oAuth2Service.handleOAuthUser(email, name, provider, providerId, oAuth2User);

            // Generate JWT token
            String token = jwtTokenProvider.generateTokenFromUser(user);

            log.info("[OAUTH2] Login successful | email={} | provider={}", email, provider);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            response.put("provider", provider);
            response.put("profilePicture", user.getProfilePicture());
            response.put("expiresIn", jwtTokenProvider.getExpirationMs());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("[OAUTH2] Login failed | error={}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "OAuth2 login failed",
                    "message", ex.getMessage()
            ));
        }
    }

    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> oauth2Failure(@RequestParam(required = false) String error) {
        log.warn("[OAUTH2] Login failed | error={}", error);

        return ResponseEntity.badRequest().body(
                oAuth2Service.handleFailure(error)
        );
    }

    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Not authenticated"
            ));
        }

        return ResponseEntity.ok(oAuth2Service.extractUserInfo(oAuth2User));
    }
}