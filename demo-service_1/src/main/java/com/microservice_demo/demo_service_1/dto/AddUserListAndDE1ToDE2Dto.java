package com.microservice_demo.demo_service_1.dto;

import com.microservice_demo.demo_service_1.entity.DemoEntity1;
import com.microservice_demo.demo_service_1.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddUserListAndDE1ToDE2Dto {
    private List<Users> usersList; // Question :- Should we transfer the entity from one service to another or not
    private DemoEntity1 de1;
}
