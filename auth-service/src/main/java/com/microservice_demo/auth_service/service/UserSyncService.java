package com.microservice_demo.auth_service.service;


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


    public void syncUserToMicroservices(Users user){
        log.info("Starting user sync to microservices for: {}", user.getUsername());
        try{
            UserSyncDto syncDto = UserSyncDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(user.getRoles())
                    .build();

            //            Demo - Service 1 Syncing
            try{
                log.info("Starting user sync to microservices for: {}", user.getUsername());
                demoService1Client.syncUser(syncDto);
                log.info("User synced successfully to Demo-Service1: {}", user.getUsername());
            }catch (Exception ex){
                log.error("Failed to sync to Demo-Service1: {} - Error: {}", user.getUsername(), ex.getMessage());
            }

            //            Demo - Service 2 Syncing
            try{
                log.debug("Syncing user to Demo-Service2...");
                demoService2Client.syncUser(syncDto);
                log.info("User synced successfully to Demo-Service2: {}", user.getUsername());
            }catch (Exception ex){
                System.out.println("Failed to sync to Demo-Service2: " + ex.getMessage());
            }
        }catch (Exception ex){
            log.error("Failed to sync to Demo-Service2: {} - Error: {}", user.getUsername(), ex.getMessage());
        }
    }
}
