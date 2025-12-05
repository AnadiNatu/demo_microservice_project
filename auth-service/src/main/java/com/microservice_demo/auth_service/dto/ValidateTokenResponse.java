package com.microservice_demo.auth_service.dto;


import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateTokenResponse {

    private boolean valid;
    private String username;
    private Set<String> roles;
    private String message;

}