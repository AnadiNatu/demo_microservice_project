package com.microservice_demo.demo_service_2.service;

import com.microservice_demo.demo_service_2.dto.*;
import com.microservice_demo.demo_service_2.entity.DemoEntity2;
import com.microservice_demo.demo_service_2.entity.Users;
import com.microservice_demo.demo_service_2.enums.EntityStatus;
import com.microservice_demo.demo_service_2.enums.UserRoles;
import com.microservice_demo.demo_service_2.exception.errors.ResourceNotFoundException;
import com.microservice_demo.demo_service_2.feign.DemoService1FeignClient;
import com.microservice_demo.demo_service_2.repository.DemoEntity2Repository;
import com.microservice_demo.demo_service_2.repository.UserRepository;
import com.microservice_demo.demo_service_2.service.interfaces.DemoEntity2ServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemoEntity2Service implements DemoEntity2ServiceInterface {
    private final DemoEntity2Repository repo;
    private final DemoService1FeignClient feign;
    private final UserRepository userRepo;



    @Override
    public DemoEntity2Dto create(CreateDemoEntity2Dto dto) {

        DemoEntity2 entity = DemoEntity2.builder()
                .demoInfo(dto.getDemoInfo())
                .entityStatus(EntityStatus.valueOf(dto.getEntityStatus()))
                .countField(dto.getCountField())
                .priceField(dto.getPriceField())
                .userIds(new ArrayList<>())
                .demoEn1Id(null)
                .build();

        return toDto(repo.save(entity));
    }

    @Override
    public DemoEntity2Dto get(Long id) {
        DemoEntity2 entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity2 not found"));

        return toDto(entity);
    }


    @Override
    public DemoEntity2Dto addUsersAndDemoEntity1(AddUserListAndDE1ToDE2Dto dto) {

        DemoEntity2 entity = repo.findById(dto.getDemoEn2Id())
                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity2 not found"));

        // store IDs only
        entity.getUserIds().addAll(dto.getUserIds());
        entity.setDemoEn1Id(dto.getDemoEn1Id());

        return toDto(repo.save(entity));
    }

    @Override
    public DemoEntity2Dto addUserToDemoEntity2(AddUserToListDE1ForDE2Dto dto) {

        DemoEntity2 entity = repo.findById(dto.getDemoEn2Id())
                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity2 not found"));

        entity.getUserIds().add(dto.getUserId());

        return toDto(repo.save(entity));
    }

    @Override
    public Users createUser(CreateUserDto dto) {
        Optional<Users> existing = userRepo.findAll().stream().filter(u -> u.getEmail().equals(dto.getEmail())).findFirst();

        if (existing.isPresent()){
            return existing.get();
        }

        Users user = new Users();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        try{
            UserRoles roleEnum = UserRoles.valueOf(
                    dto.getUserRole().replace("ROLE_" , "").toUpperCase());

            user.setRole(roleEnum);
        }catch (Exception ex){
            user.setRole(UserRoles.USER);
        }

        user.setDe1ConnectionFlag(false);
        user.setDe2ConnectionFlag(false);

        return userRepo.save(user);
    }

    @Override
    public Users getUser(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private DemoEntity2Dto toDto(DemoEntity2 entity) {

        DemoEntity2Dto dto = new DemoEntity2Dto();

        dto.setDemoEn2Id(entity.getDemoEn2Id());
        dto.setDemoInfo(entity.getDemoInfo());
        dto.setEntityStatus(entity.getEntityStatus().name());
        dto.setCountField(entity.getCountField());
        dto.setPriceField(entity.getPriceField());

        List<Long> ids = entity.getUserIds();
        List<UserDto> users = feign.getUsersByIdList(ids);

        dto.setUserId(ids);

        if (users != null && !users.isEmpty()) {
            dto.setUserName(users.stream().map(UserDto::getName).toList());
        } else {
            dto.setUserName(new ArrayList<>());
        }

        if (entity.getDemoEn1Id() != null) {
            DemoEntity1Dto de1 = feign.getDemoEntity1ForEn2(entity.getDemoEn1Id());
            dto.setDe1Id(de1.getDemoEn1Id());
        }

        return dto;
    }
}
