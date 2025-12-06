package com.microservice_demo.demo_service_1.mapper;

import com.microservice_demo.demo_service_1.dto.DemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.UserDto;
import com.microservice_demo.demo_service_1.entity.DemoEntity1;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.enums.EntityStatus;
import com.microservice_demo.demo_service_1.enums.UserRoles;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class DemoService1Mapper {
//
//    public UserDto toUserDto(Users users){
//
//        UserDto userDto = new UserDto();
//
//        userDto.setUserId(users.getUserId());
//        userDto.setName(users.getName());
//        userDto.setEmail(users.getEmail());
//        userDto.setPhone(users.getPhone());
//        userDto.setUserRole(mapStatusToString(users.getRole()));
//        userDto.setDe1ConnectionFlag(true);
//        userDto.setDe2ConnectionFlag(true);
//
//        return userDto;
//    }
//
//    public DemoEntity1Dto toDE1Dto(DemoEntity1 de1){
//
//        DemoEntity1Dto de1Dto = new DemoEntity1Dto();
//
//        de1Dto.setDemoEn1Id(de1.getDemoEn1Id());
//        de1Dto.setDemoData(de1.getDemoData());
//        de1Dto.setCreatedOn(convertLocalDateToDate(de1.getCreatedOn()));
//        de1Dto.setUpdatedOn(convertLocalDateToDate(de1.getUpdatedOn()));
//
//        return de1Dto;
//    }
//
//// Make functions in this service to send the ids of the users and the demo entity and user id list and use them for the feign client method in demo-service2 and make functions in the DemoEntity2 using those feign methods . I want you to populate the updated DemoEntity feilds using those feign interface methods and return DemoEntity2Dto in . Update and create the necessary functions in the DemoEntity1Service , DemoEntity1Controller , the DemoService1FeignClient in DemoService2 and similarly update the DemoEntity2Service and DemoService2Controller
//    private String mapStatusToString(UserRoles userRoles){
//        return (switch (userRoles) {
//            case ADMIN -> "ADMIN";
//            case USER -> "USER";
//        });
//    }
//
//    private UserRoles mapStringToStatus(String status){
//        return switch (status.toUpperCase()){
//            case "ADMIN" -> UserRoles.ADMIN;
//            case "USER" -> UserRoles.USER;
//            default -> throw new IllegalStateException("Unexpected value: " + status.toUpperCase());
//        };
//    }
//
//    private Date convertLocalDateToDate(LocalDateTime date){
//        if (date == null){
//            return null;
//        }
//        return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
//    }
}
