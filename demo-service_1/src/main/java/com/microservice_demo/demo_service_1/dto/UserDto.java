package com.microservice_demo.demo_service_1.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String userRole;
    private boolean de1ConnectionFlag;
    private boolean de2ConnectionFlag;
}
