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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
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
        return safeToDto(repo.save(entity));
    }

    @Override
    public DemoEntity2Dto get(Long id) {
        DemoEntity2 entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity2 not found"));
        return safeToDto(entity);
    }

    @Override
    public DemoEntity2Dto addUsersAndDemoEntity1(AddUserListAndDE1ToDE2Dto dto) {

        DemoEntity2 entity = repo.findById(dto.getDemoEn2Id())
                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity2 not found"));

        // ✅Validate Users locally
        List<Long> validUsers = dto.getUserIds().stream()
                .filter(userRepo::existsById)
                .toList();

        if (validUsers.isEmpty()) {
            throw new RuntimeException("No valid users found. Sync required.");
        }

        // Validate DemoEntity1 remotely
        try {
            feign.getDemoEntity1ForEn2(dto.getDemoEn1Id());
        } catch (Exception ex) {
            throw new RuntimeException("DemoEntity1 not available. Sync required.");
        }

        entity.getUserIds().addAll(validUsers);
        entity.setDemoEn1Id(dto.getDemoEn1Id());

        return safeToDto(repo.save(entity));
    }

    @Override
    public DemoEntity2Dto addUserToDemoEntity2(AddUserToListDE1ForDE2Dto dto) {
        DemoEntity2 entity = repo.findById(dto.getDemoEn2Id())
                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity2 not found"));
        entity.getUserIds().add(dto.getUserId());
        return safeToDto(repo.save(entity));
    }

    @Override
    @Transactional
    public Users createUser(CreateUserDto dto, Long userId) {
        Users user = null;

        // Prefer lookup by explicit PK (most reliable)
        if (userId != null) {
            user = userRepo.findById(userId).orElse(null);
        }
        // Fallback: lookup by email (covers re-sync scenarios)
        if (user == null && dto.getEmail() != null) {
            user = userRepo.findByEmail(dto.getEmail()).orElse(null);
        }

        boolean isNew = (user == null);
        if (isNew) {
            user = new Users();
            if (userId != null) {
                // Force-set the PK so JPA inserts with the auth-service ID, not a generated one
                user.setUserId(userId);
            }
            user.setDe1ConnectionFlag(false);
            user.setDe2ConnectionFlag(false);
        }

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone() != null ? dto.getPhone() : "");

        // Map role string ("USER" or "ADMIN") → UserRoles enum
        try {
            String roleStr = dto.getUserRole() != null
                    ? dto.getUserRole().replace("ROLE_", "").toUpperCase()
                    : "USER";
            user.setRole(UserRoles.valueOf(roleStr));
        } catch (Exception ex) {
            log.warn("[DS2] Unknown role '{}', defaulting to USER", dto.getUserRole());
            user.setRole(UserRoles.USER);
        }

        Users saved = userRepo.save(user);
        log.info("[DS2] User {} | id={} email={}", isNew ? "created" : "updated",
                saved.getUserId(), saved.getEmail());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Users getUser(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public void updateProfilePicture(Long userId , String profilePicture){
        Users user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found exception" + userId));

        user.setProfilePicture(profilePicture);
        userRepo.save(user);

        log.info("✅ [DS1] Profile picture updated | userId={} | url={}", userId,
                profilePicture != null ? profilePicture : "removed");
    }

//    Helper Methods

    private DemoEntity2Dto safeToDto(DemoEntity2 entity) {

        DemoEntity2Dto dto = new DemoEntity2Dto();

        dto.setDemoEn2Id(entity.getDemoEn2Id());
        dto.setDemoInfo(entity.getDemoInfo());
        dto.setEntityStatus(entity.getEntityStatus().name());
        dto.setCountField(entity.getCountField());
        dto.setPriceField(entity.getPriceField());

       List<Long> ids = entity.getUserIds() != null ? entity.getUserIds() : new ArrayList<>();
        dto.setUserId(ids);

        if (ids.isEmpty()) {
            dto.setUserName(new ArrayList<>());
        } else {
            try {
                List<UserDto> users = feign.getUsersByIdList(ids);
                dto.setUserName(users.stream().map(UserDto::getName).toList());
            } catch (Exception ex) {
                log.warn("[DS2] User fetch failed → fallback");
                dto.setUserName(new ArrayList<>());
            }
        }
        if (entity.getDemoEn1Id() != null) {
            try {
                DemoEntity1Dto de1 = feign.getDemoEntity1ForEn2(entity.getDemoEn1Id());
                dto.setDe1Id(de1.getDemoEn1Id());
            } catch (Exception ex) {
                log.warn("[DS2] DemoEntity1 fetch failed");
            }
        }
        return dto;
    }
}