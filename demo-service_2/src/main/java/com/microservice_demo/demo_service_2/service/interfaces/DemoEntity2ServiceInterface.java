package com.microservice_demo.demo_service_2.service.interfaces;

import com.microservice_demo.demo_service_2.dto.AddUserListAndDE1ToDE2Dto;
import com.microservice_demo.demo_service_2.dto.AddUserToListDE1ForDE2Dto;
import com.microservice_demo.demo_service_2.dto.CreateDemoEntity2Dto;
import com.microservice_demo.demo_service_2.dto.DemoEntity2Dto;

public interface DemoEntity2ServiceInterface {

    public DemoEntity2Dto create(CreateDemoEntity2Dto dto);
    public DemoEntity2Dto get(Long id);
    public DemoEntity2Dto addUsersAndDemoEntity1(AddUserListAndDE1ToDE2Dto dto);
    public DemoEntity2Dto addUserToDemoEntity2(AddUserToListDE1ForDE2Dto dto);

}
