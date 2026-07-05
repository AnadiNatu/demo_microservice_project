package com.microservice_demo.auth_service.service;


import com.microservice_demo.auth_service.dto.ProfilePictureSyncDto;
import com.microservice_demo.auth_service.dto.UserSyncDto;
import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.feign.DemoService1FeignClient;
import com.microservice_demo.auth_service.feign.DemoService2FeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

//Made specifically for OAuthServiceImpl because the dependency on AuthService was causing the circular dependency error
//Handles user synchronisation from Auth-Service to Demo-Service1 and Demo-Service2.
//Extracted into its own bean to avoid circular dependency with AuthService/OAuthServiceImpl.
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

    private final DemoService1FeignClient demoService1Client;
    private final DemoService2FeignClient demoService2Client;

    private static final int DS2_MAX_ATTEMPTS = 6;
    private static final long DS2_INITIAL_DELAY = 5_000L;
    private static double DS2_BACKOFF_MULT = 1.5;

    public void syncToMicroservices(Users user){
        log.info("[SYNC] Starting user sync | username={}" , user.getUsername());

        UserSyncDto syncDto = buildSyncDto(user);

        syncToDS1(syncDto);

        syncToDS2Async(syncDto);
    }

    public void syncProfilePictureUpdate(Long userId , String profilePictureUrl){
        log.info("[SYNC] Syncing profile-picture | userId={}" , userId);

        ProfilePictureSyncDto syncDto = ProfilePictureSyncDto.builder()
                .userId(userId)
                .profilePictureUrl(profilePictureUrl)
                .build();

        try{
            demoService1Client.syncProfilePicture(syncDto);
            log.info("[SYNC] Profile-picture synced to DS1 | userId={}", userId);
        }catch (Exception ex){
            log.error("[SYNC] DS1 profile-picture sync failed | userId={} | error={}" , userId , ex.getMessage());
        }

        syncProfilePictureToDS2Async(syncDto);
    }

    private void syncToDS1(UserSyncDto dto){
        try{
            demoService1Client.syncUser(dto);
            log.info("[SYNC] User synced to Demo-Service1 | username={}" , dto.getUsername());
        }catch (Exception ex){
            log.error("[SYNC] Demo-service1 sync failed | username = {} | error = {} " ,   dto.getUsername(), ex.getMessage());
        }
    }

    @Async
    public void syncToDS2Async(UserSyncDto dto){
        long delayMs = DS2_INITIAL_DELAY;

        for(int attempt = 1 ; attempt <= DS2_MAX_ATTEMPTS ; attempt++){
            try{
                demoService2Client.syncUser(dto);
                log.info("[SYNC] User synced to Demo-Service2 | username = {} | attmpt = {}" , dto.getUsername() , attempt);
                return;
            }catch (Exception ex){
                boolean isConnRefused = isConnectionRefused(ex);

                if (isConnRefused && attempt < DS2_MAX_ATTEMPTS){
                    log.warn("[SYNC] Demo-Service2 not ready yet (attempt {} / {}) - retryingin {}ms | error={}" , attempt , DS2_MAX_ATTEMPTS , delayMs , ex.getMessage());
                    sleep(delayMs);
                    delayMs = (long) (delayMs * DS2_BACKOFF_MULT);
                }else {
                    log.error("[SYNC] Demo-Service2 sync FAILED permanently | username={} | attempt={} | error={}",
                            dto.getUsername(), attempt, ex.getMessage());
                    return;
                }
            }
        }
    }

    @Async
    public void syncProfilePictureToDS2Async(ProfilePictureSyncDto dto) {
        long delayMs = DS2_INITIAL_DELAY;

        for (int attempt = 1; attempt <= DS2_MAX_ATTEMPTS; attempt++) {
            try {
                demoService2Client.syncProfilePicture(dto);
                log.info("[SYNC] Profile-picture synced to DS2 | userId={} (attempt {})",
                        dto.getUserId(), attempt);
                return;
            } catch (Exception ex) {
                boolean isConnRefused = isConnectionRefused(ex);

                if (isConnRefused && attempt < DS2_MAX_ATTEMPTS) {
                    log.warn("[SYNC] DS2 not ready for profile-picture sync (attempt {}/{}) — retrying in {} ms",
                            attempt, DS2_MAX_ATTEMPTS, delayMs);
                    sleep(delayMs);
                    delayMs = (long) (delayMs * DS2_BACKOFF_MULT);
                } else {
                    log.error("[SYNC] DS2 profile-picture sync FAILED permanently | userId={} | error={}",
                            dto.getUserId(), ex.getMessage());
                    return;
                }
            }
        }
    }

    private UserSyncDto buildSyncDto(Users user){
        return UserSyncDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .profilePicture(user.getProfilePicture())
                .build();
    }

    private boolean isConnectionRefused(Exception ex){
        Throwable cause = ex;
        while(cause != null){
            if (cause instanceof java.net.ConnectException) return true;
            if (cause.getMessage() != null && (cause.getMessage().contains("Connection refused") || cause.getMessage().contains("Connect timed out"))) return true;

            cause = cause.getCause();
        }
        return false;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
//    public void syncUserToMicroservices(Users user) {
//        log.info("[SYNC] Starting user sync | username={}", user.getUsername());
//
//        UserSyncDto syncDto = UserSyncDto.builder()
//                .id(user.getId())
//                .username(user.getUsername())
//                .email(user.getEmail())
//                .phoneNumber(user.getPhoneNumber())
//                .roles(user.getRoles())
//                .profilePicture(user.getProfilePicture())
//                .build();
//
//        try {
//            demoService1Client.syncUser(syncDto);
//            log.info("[SYNC] User synced to Demo-Service1 | username={}", user.getUsername());
//        } catch (Exception ex) {
//            log.error("[SYNC] Demo-Service1 sync failed | username={} | error={}", user.getUsername(), ex.getMessage());
//        }
//
//        try {
//            demoService2Client.syncUser(syncDto);
//            log.info("[SYNC] User synced to Demo-Service2 | username={}", user.getUsername());
//        } catch (Exception ex) {
//            log.error("[SYNC] Demo-Service2 sync failed | username={} | error={}", user.getUsername(), ex.getMessage());
//        }
//    }
//
//    public void syncProfilePictureUpdate(Long userId , String profilePictureUrl){
//        log.info("[SYNC] Syncing profile-picture | userId={}" , userId);
//        try{
//            ProfilePictureSyncDto syncDto = ProfilePictureSyncDto.builder()
//                    .userId(userId)
//                    .profilePictureUrl(profilePictureUrl)
//                    .build();
//
//            demoService1Client.syncProfilePicture(syncDto);
//            demoService2Client.syncProfilePicture(syncDto);
//            log.info("[SYNC] Profile-picture synced successfully | userId={}", userId);
//
//        } catch (Exception ex) {
//
//            log.error("[SYNC] Profile-picture sync failed | userId={} | error={}",
//                    userId, ex.getMessage());
//        }
//    }
}
