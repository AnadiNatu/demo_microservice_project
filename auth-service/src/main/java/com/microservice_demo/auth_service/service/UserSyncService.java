package com.microservice_demo.auth_service.service;


import com.microservice_demo.auth_service.dto.ProfilePictureSyncDto;
import com.microservice_demo.auth_service.dto.UserSyncDto;
import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.feign.DemoService1FeignClient;
import com.microservice_demo.auth_service.feign.DemoService2FeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//Made specifically for OAuthServiceImpl because the dependency on AuthService was causing the circular dependency error
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

    private final DemoService1FeignClient demoService1Client;
    private final DemoService2FeignClient demoService2Client;

    public void syncUserToMicroservices(Users user) {
        log.info("[SYNC] Starting user sync | username={}", user.getUsername());

        UserSyncDto syncDto = UserSyncDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .profilePicture(user.getProfilePicture())
                .build();

        try {
            demoService1Client.syncUser(syncDto);
            log.info("[SYNC] User synced to Demo-Service1 | username={}", user.getUsername());
        } catch (Exception ex) {
            log.error("[SYNC] Demo-Service1 sync failed | username={} | error={}", user.getUsername(), ex.getMessage());
        }

        try {
            demoService2Client.syncUser(syncDto);
            log.info("[SYNC] User synced to Demo-Service2 | username={}", user.getUsername());
        } catch (Exception ex) {
            log.error("[SYNC] Demo-Service2 sync failed | username={} | error={}", user.getUsername(), ex.getMessage());
        }
    }

    public void syncProfilePictureUpdate(Long userId , String profilePictureUrl){
        log.info("[SYNC] Syncing profile-picture | userId={}" , userId);
        try{
            ProfilePictureSyncDto syncDto = ProfilePictureSyncDto.builder()
                    .userId(userId)
                    .profilePictureUrl(profilePictureUrl)
                    .build();

            demoService1Client.syncProfilePicture(syncDto);
            demoService2Client.syncProfilePicture(syncDto);
            log.info("[SYNC] Profile-picture synced successfully | userId={}", userId);

        } catch (Exception ex) {

            log.error("[SYNC] Profile-picture sync failed | userId={} | error={}",
                    userId, ex.getMessage());
        }
    }
}
