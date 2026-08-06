package com.microservice_demo.auth_service.dto;

import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncDto {

    private Long userId;
    private String username;
    private String email;
    private String phone;
    private Set<String> role;
    private String profilePicture;
}
