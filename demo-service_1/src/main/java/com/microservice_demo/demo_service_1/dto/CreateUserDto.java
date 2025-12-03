package com.microservice_demo.demo_service_1.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserDto {

    private String name;
    private String email;
    private String phone;
    private String userRole;

}
