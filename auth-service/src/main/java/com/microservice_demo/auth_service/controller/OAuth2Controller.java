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
@RequestMapping("/api/oauth2")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final JwtTokenProvider jwtTokenProvider;

    private final OAuth2ServiceImpl oAuth2Service;

    // SUCCESS ENDPOINT
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> oauth2Success(@AuthenticationPrincipal OAuth2User oAuth2User) {

        if (oAuth2User == null) {
            log.error("[OAUTH2] No OAuth2 principal found");

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                                    "error",
                                    "Authentication failed",

                                    "message",
                                    "No user information received from OAuth2 provider"
                            ));
        }

        try {

            String email = oAuth2User.getAttribute("email");

            String name = oAuth2User.getAttribute("name");
            String provider = oAuth2Service.determineProvider(oAuth2User);

            // PROVIDER ID

            String providerId = oAuth2User.getAttribute("sub");

            // GitHub fallback
            if (providerId == null) {
                providerId = String.valueOf(oAuth2User.getAttribute("id"));
            }

            log.info("[OAUTH2] Login attempt | provider={} | email={}", provider, email);

            // HANDLE USER
            Users user =
                    oAuth2Service.handleOAuthUser(
                            email,
                            name,
                            provider,
                            providerId,
                            oAuth2User
                    );

            // TOKENS

            String token = jwtTokenProvider.generateTokenFromUser(user);

            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

            log.info("[OAUTH2] Login successful | email={} | provider={}", email, provider);

            Map<String, Object> response = new HashMap<>();

            response.put("token", token);
            response.put("refreshToken", refreshToken);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            response.put("provider", provider);
            response.put("profilePicture", user.getProfilePicture());

            response.put("expiresIn", jwtTokenProvider.getExpirationMs());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {

            log.error("[OAUTH2] Login failed | error={}", ex.getMessage(), ex);
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                                    "error",
                                    "OAuth2 login failed",
                                    "message", ex.getMessage()));
        }
    }

    // FAILURE ENDPOINT
    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> oauth2Failure(@RequestParam(required = false) String error) {

        log.warn("[OAUTH2] Login failed | error={}", error);

        return ResponseEntity
                .badRequest()
                .body(oAuth2Service.handleFailure(error));
    }

    // USER INFO
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(@AuthenticationPrincipal OAuth2User oAuth2User) {

        if (oAuth2User == null) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Not authenticated"));
        }

        return ResponseEntity.ok(oAuth2Service.extractUserInfo(oAuth2User));
    }
}