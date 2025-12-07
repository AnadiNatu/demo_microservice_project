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

    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
}
