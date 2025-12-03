package com.microservice_demo.demo_service_2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserDto {

    private String name;
    private String email;
    private String phone;
    private String userRole;

}
