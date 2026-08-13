package com.microservice_demo.demo_service_1.kafka;


import com.microservice_demo.demo_service_1.dto.CreateUserDto;
import com.microservice_demo.demo_service_1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserService userService;

    @KafkaListener(
            topics = "user-registered",
            groupId = "demo-service1-group",
            containerFactory = "userEventListenerFactory"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("[KAFKA][DS1] Received UserRegisteredEvent | userId={} email={}", event.getUserId(), event.getEmail());
        try {
            CreateUserDto createDto = CreateUserDto.builder()
                    .name(event.getUsername())
                    .email(event.getEmail())
                    .phone(event.getPhone())
                    .userRole(event.getRoles() != null && !event.getRoles().isEmpty()
                            ? event.getRoles().iterator().next()
                            : "ROLE_USER")
                    .build();

            userService.createUser(createDto, event.getUserId());
            log.info("[KAFKA][DS1] User synced via Kafka | userId={}", event.getUserId());
        } catch (Exception ex) {
            log.error("[KAFKA][DS1] Failed to process UserRegisteredEvent | userId={} | error={}", event.getUserId(), ex.getMessage());
        }
    }
}