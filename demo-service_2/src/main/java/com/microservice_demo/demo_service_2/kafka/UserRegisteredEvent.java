package com.microservice_demo.demo_service_2.kafka;

import lombok.*;

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
