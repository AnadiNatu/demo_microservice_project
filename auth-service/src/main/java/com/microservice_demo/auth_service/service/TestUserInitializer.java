package com.microservice_demo.auth_service.service;

import com.microservice_demo.auth_service.entity.Users;
import com.microservice_demo.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.test-users.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class TestUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSyncService userSyncService;

    @Override
    public void run(ApplicationArguments args){
        log.info("========================================================");
        log.info("Initializing Demo/Test Users");
        log.info("========================================================");

        createUser(
                "admin",
                "anadINatu2001+admin@gmail.com",
                "8318428125",
                "black_admin@1",
                Set.of("ROLE_ADMIN")
        );

        createUser(
                "user",
                "anadINatu2001+user@gmail.com",
                "8707625812",
                "black_user@1",
                Set.of("ROLE_USER")
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Users createUser(String username , String email , String phone , String password , Set<String> roles){
        Optional<Users> existing = userRepository.findByUsername(email);

        if(existing.isEmpty()){
            existing = userRepository.findByEmail(email);
        }

        if (existing.isPresent()){
            Users existingUser = existing.get();

            log.info("User already exists -> {}. Synchronizing...", username);

            try{
                userSyncService.syncToMicroservices(existingUser);
            }catch (Exception ex){
                log.error("Synchronization failed for existing user {}", username,ex);
            }

            return existingUser;
        }

        Users user = Users.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .phoneNumber(phone)
                .roles(roles)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .provider("LOCAL")
                .profilePicture(null)
                .build();

        Users saved = userRepository.save(user);
        log.info("Created new user -> {}", username);

        try{
            userSyncService.syncToMicroservices(saved);
        }catch (Exception ex){
            log.error(
                    "Synchronization failed for newly created user {}", username, ex);
        }

        return saved;
    }

    private void synchronizeAllUsers(){
        log.info("========================================================");
        log.info("Running Full User Synchronization");
        log.info("========================================================");

        List<Users> users = userRepository.findAll();

        for (Users user : users){
            try{
                log.info("Synchronization {} " , user.getUsername());
                userSyncService.syncToMicroservices(user);
            }catch (Exception ex){
                log.error("Synchronization failed for {}", user.getUsername(), ex);
            }
        }
        log.info("Full synchronization completed.");
    }

    private void validateUsers() {
        long totalUsers = userRepository.count();

        log.info("========================================================");
        log.info("Validation");
        log.info("========================================================");
        log.info("Total Users in Auth-Service : {}", totalUsers);

        userRepository.findAll().forEach(user -> log.info("ID={} | USERNAME={} | EMAIL={} | ROLES={}", user.getId(), user.getUsername(), user.getEmail(), user.getRoles()));
        log.info("Validation completed.");
    }
}
