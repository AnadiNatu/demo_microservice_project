package com.microservice_demo.demo_service_1.dto;

import com.microservice_demo.demo_service_1.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddUserToListDE1ForDE2Dto {

    private Users user;
    private Long demoEn1Id;
}
