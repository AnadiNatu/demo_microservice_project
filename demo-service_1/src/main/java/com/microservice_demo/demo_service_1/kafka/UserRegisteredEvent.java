package com.microservice_demo.demo_service_1.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent implements Serializable {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private Set<String> roles;
    private String profilePicture;
}
