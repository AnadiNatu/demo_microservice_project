package com.microservice_demo.demo_service_1.service;

import com.microservice_demo.demo_service_1.dto.CreateDemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.DemoEntity1Dto;
import com.microservice_demo.demo_service_1.dto.UserDto;
import com.microservice_demo.demo_service_1.entity.DemoEntity1;
import com.microservice_demo.demo_service_1.entity.Users;
import com.microservice_demo.demo_service_1.enums.EntityStatus;
import com.microservice_demo.demo_service_1.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_1.repository.DemoEntity1Repository;
import com.microservice_demo.demo_service_1.repository.UserRepository;
import com.microservice_demo.demo_service_1.service.interfaces.DemoEntity1ServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoEntity1Service implements DemoEntity1ServiceInterface {

    private final DemoEntity1Repository repo;
    private final UserRepository usersRepo;

    @Override
    public DemoEntity1Dto create(CreateDemoEntity1Dto request) {
        Users user = usersRepo.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        DemoEntity1 entity = new DemoEntity1();
        entity.setDemoData(request.getDemoData());
        entity.setUser(user);

        user.setDe1ConnectionFlag(true);
        usersRepo.save(user);

        DemoEntity1 saved = repo.save(entity);

        return toDto(saved);
    }

    @Override
    public DemoEntity1Dto getEntity(Long id) {
        DemoEntity1 entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity1 not found: " + id));

        return toDto(entity);
    }

    @Override
    public DemoEntity1Dto getDemoEntity1(Long id) {
        return getEntity(id);
    }

    @Override
    public DemoEntity1Dto getDemoEntity1ByUserId(Long userId) {
        DemoEntity1 entity = repo.findByUserUserId(userId);
        if (entity == null)
            throw new ResourceNotFoundException("DemoEntity1 not found for userId: " + userId);

        return toDto(entity);
    }

    // NEW METHOD — used by DemoService2
    @Override
    public List<UserDto> getUsersByIds(List<Long> userIds) {

        List<Users> users = usersRepo.findAllById(userIds);
//        if(users.isEmpty()) throw new ResourceNotFoundException("No users found");

        List<UserDto> dtos = new ArrayList<>();

        for(Users u : users) {
            UserDto dto = new UserDto();
            dto.setUserId(u.getUserId());
            dto.setName(u.getName());
            dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone());
            dto.setUserRole(u.getRole().name());
            dto.setDe1ConnectionFlag(u.isDe1ConnectionFlag());
            dto.setDe2ConnectionFlag(u.isDe2ConnectionFlag());
            dtos.add(dto);
        }

        return dtos;
    }

    private DemoEntity1Dto toDto(DemoEntity1 en1) {
        DemoEntity1Dto dto = new DemoEntity1Dto();
        dto.setDemoEn1Id(en1.getDemoEn1Id());
        dto.setDemoData(en1.getDemoData());
        dto.setCreatedOn(convertLocalDateToDate(en1.getCreatedOn()));
        dto.setUpdatedOn(convertLocalDateToDate(en1.getUpdatedOn()));
        return dto;
    }

    private Date convertLocalDateToDate(LocalDateTime date){
        if (date == null){
            return null;
        }
        return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime convertDateToLocalDate(Date date){
        if (date == null){
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String mapStatusToString(EntityStatus entityStatus){
        return (switch (entityStatus) {
            case STATUS1 -> "Status1";
            case STATUS2 -> "Status2";
            case STATUS3 -> "Status3";
        });
    }

    private EntityStatus mapStringToStatus(String status){
        return switch (status.toUpperCase()){
            case "STATUS1" -> EntityStatus.STATUS1;
            case "STATUS2" -> EntityStatus.STATUS2;
            default-> EntityStatus.STATUS3;
        };
    }

}
