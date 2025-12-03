package com.microservice_demo.demo_service_2.dto;


import com.microservice_demo.demo_service_2.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUserToListDE1ForDE2Dto {
    private Long userId;
    private Long demoEn2Id;
}
