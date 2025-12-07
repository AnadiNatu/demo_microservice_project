package com.microservice_demo.demo_service_2.service.interfaces;

import com.microservice_demo.demo_service_2.dto.*;
import com.microservice_demo.demo_service_2.entity.Users;

public interface DemoEntity2ServiceInterface {

    public DemoEntity2Dto create(CreateDemoEntity2Dto dto);
    public DemoEntity2Dto get(Long id);
    public DemoEntity2Dto addUsersAndDemoEntity1(AddUserListAndDE1ToDE2Dto dto);
    public DemoEntity2Dto addUserToDemoEntity2(AddUserToListDE1ForDE2Dto dto);
    public Users createUser(CreateUserDto dto);
    public Users getUser(Long id);
}
