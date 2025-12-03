package com.microservice_demo.demo_service_2.dto;


import com.microservice_demo.demo_service_2.entity.DemoEntity1;
import com.microservice_demo.demo_service_2.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUserListAndDE1ToDE2Dto {
    private Long demoEn2Id;
    private List<Long> userIds;
    private Long demoEn1Id;
}
