package com.microservice_demo.demo_service_2.kafka;

import com.microservice_demo.demo_service_2.service.UserService;
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
            groupId = "demo-service2-group",
            containerFactory = "userEventListenerFactory"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("[KAFKA][DS2] Received UserRegisteredEvent | userId={} email={}", event.getUserId(), event.getEmail());
        try {
            userService.syncUser(
                    event.getUserId(),
                    event.getUsername(),
                    event.getEmail(),
                    event.getPhone(),
                    event.getRoles()
            );
            log.info("[KAFKA][DS2] User synced via Kafka | userId={}", event.getUserId());
        } catch (Exception ex) {
            log.error("[KAFKA][DS2] Failed to process UserRegisteredEvent | userId={} | error={}", event.getUserId(), ex.getMessage());
        }

    }
}