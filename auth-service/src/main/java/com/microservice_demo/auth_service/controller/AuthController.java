package com.microservice_demo.auth_service.controller;

import com.microservice_demo.auth_service.dto.*;
import com.microservice_demo.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validateToken(request));
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(new MessageResponse("Auth Service is running"));
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String,Object>> test(){
        Map<String,Object> response = new HashMap<>();
        response.put("status","success");
        response.put("message" , "Auth Service is running - Test endpoint");
        response.put("service" , "auth-service");
        response.put("timestamp" , LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
