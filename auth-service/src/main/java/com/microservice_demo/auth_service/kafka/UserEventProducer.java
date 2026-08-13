package com.microservice_demo.auth_service.kafka;


import com.microservice_demo.auth_service.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, UserRegisteredEvent> userEventKafkaTemplate;

    // KAFKA: Publish UserRegisteredEvent after successful user creation. Runs alongside the existing Feign sync, does not replace it.
    public void publishUserRegistered(Users user) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .roles(user.getRoles())
                .profilePicture(user.getProfilePicture())
                .build();

        userEventKafkaTemplate.send(KafkaTopics.USER_REGISTERED, String.valueOf(user.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to publish UserRegisteredEvent | userId={} | error={}", user.getId(), ex.getMessage());
                    } else {
                        log.info("[KAFKA] UserRegisteredEvent published | userId={} | partition={} offset={}",
                                user.getId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }

}
