package com.microservice_demo.demo_service_2.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String userRole;
    private boolean de1ConnectionFlag;
    private boolean de2ConnectionFlag;


}
