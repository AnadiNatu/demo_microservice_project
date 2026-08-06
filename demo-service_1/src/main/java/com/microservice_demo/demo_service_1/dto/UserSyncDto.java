package com.microservice_demo.demo_service_1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncDto {
    private Long userId;
    private String name;
    private String email;
    private String phone;

    // FIXED: Changed from String userRole to Set<String> role for consistency
    private Set<String> role;

    // Additional fields for profile sync
    private String username;
    private String provider;
    private String providerId;
}
