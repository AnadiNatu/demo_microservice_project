package com.microservice_demo.auth_service.dto;

import lombok.*;

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
