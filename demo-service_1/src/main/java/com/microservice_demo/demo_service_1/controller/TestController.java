package com.microservice_demo.demo_service_1.controller;

import com.microservice_demo.demo_service_1.dto.TestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/en1/test")
@CrossOrigin("*")
@Slf4j
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<TestResponse> publicEndPoint() {
        log.info("[TestController] Public endpoint accessed");
        TestResponse response = new TestResponse(
                "success",
                "Public endpoint - No authentication required",
                "demo-service-1",
                null,
                null,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/protected")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TestResponse> protectedEndPoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("[TestController] Protected endpoint accessed by: {}", auth.getName());

        TestResponse response = new TestResponse(
                "success",
                "Protected endpoint - Requires authentication",
                "demo-service-1",
                auth.getName(),
                auth.getAuthorities().toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TestResponse> userEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("[TestController] User-only endpoint accessed by: {}", auth.getName());

        TestResponse response = new TestResponse(
                "success",
                "User-only endpoint - Requires USER role",
                "demo-service-1",
                auth.getName(),
                auth.getAuthorities().toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestResponse> adminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("[TestController] Admin-only endpoint accessed by: {}", auth.getName());

        TestResponse response = new TestResponse(
                "success",
                "Admin-only endpoint - Requires ADMIN role",
                "demo-service-1",
                auth.getName(),
                auth.getAuthorities().toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/data")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createData(@RequestBody Map<String, String> data) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("[TestController] Create data endpoint accessed by: {}", auth.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Data created successfully");
        response.put("service", "demo-service-1");
        response.put("receivedData", data);
        response.put("createdBy", auth.getName());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/whoami")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> whoAmI() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("[TestController] WhoAmI endpoint accessed by: {}", auth.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("service", "demo-service-1");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("authenticated", auth.isAuthenticated());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}