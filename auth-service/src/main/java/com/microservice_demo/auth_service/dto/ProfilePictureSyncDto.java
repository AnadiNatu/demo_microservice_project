package com.microservice_demo.auth_service.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePictureSyncDto {
    private Long userId;
    private String profilePictureUrl;
}
