package com.microservice_demo.demo_service_1.dto.functionality;

import lombok.*;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Builder
public class ProfilePictureSyncDto {
    private Long userId;
    private String profilePictureUrl;
}
