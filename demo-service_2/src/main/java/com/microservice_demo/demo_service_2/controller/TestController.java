package com.microservice_demo.demo_service_2.controller;

import com.microservice_demo.demo_service_2.dto.TestResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/en2/test")
@CrossOrigin("*")
public class TestController {

    @GetMapping("/public")
    public TestResponse publicEndPoint(){
        return new TestResponse(
                "success",
                "Public endpoint - No authentication required",
                "demo-service2",
                null,
                null,
                LocalDateTime.now()
        );
    }

    @GetMapping("/protected")
    @PreAuthorize("hasAnyRole('USER' ,'ADMIN')")
    public TestResponse protectedEndPoint(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return new TestResponse(
                "success",
                "Protected endpoint - Requires authentication",
                "demo-service1",
                auth.getName(),
                auth.getAuthorities().toString(),
                LocalDateTime.now()
        );
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public TestResponse userEndpoint(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return new TestResponse(
                "Success",
                "User-only endpoint - Requires USER role",
                "demo-service1",
                auth.getName(),
                auth.getAuthorities().toString(),
                LocalDateTime.now()
        );
    }

    @PostMapping("/data")
    @PreAuthorize("hasAnyRole('USER' , 'ADMIN')")
    public Map<String , Object> createData(@RequestBody Map<String , String> data){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String , Object> response = new HashMap<>();
        response.put("status" , "success");
        response.put("message", "Data created successfully");
        response.put("service", "demo-service1");
        response.put("receivedData", data);
        response.put("createdBy", auth.getName());
        response.put("timestamp", LocalDateTime.now());

        return response;
    }

    @GetMapping("/whoami")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Map<String, Object> whoAmI() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("service", "demo-service1");
        response.put("username", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("authenticated", auth.isAuthenticated());
        response.put("timestamp", LocalDateTime.now());

        return response;
    }

}
