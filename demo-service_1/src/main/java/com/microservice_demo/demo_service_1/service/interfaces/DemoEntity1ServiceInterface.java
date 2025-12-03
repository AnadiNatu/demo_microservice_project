package com.microservice_demo.demo_service_1.service.interfaces;

import com.microservice_demo.demo_service_1.dto.CreateDemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.DemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.UserDto;
import org.springframework.stereotype.Service;

import java.util.List;


public interface DemoEntity1ServiceInterface {

    DemoEntity1Dto create(CreateDemoEntity1Dto request);
    DemoEntity1Dto getEntity(Long id);
    DemoEntity1Dto getDemoEntity1ByUserId(Long id);
    // NEW: return list of Users by ID for Feign usage
    List<UserDto> getUsersByIds(List<Long> userIds);

    // NEW: return DemoEntity1 by ID (reused by DemoService2)
    DemoEntity1Dto getDemoEntity1(Long id);
}
