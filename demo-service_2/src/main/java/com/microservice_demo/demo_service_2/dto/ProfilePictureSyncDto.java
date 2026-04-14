package com.microservice_demo.demo_service_2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePictureSyncDto {
    private Long userId;
    private String profilePictureUrl;
}
