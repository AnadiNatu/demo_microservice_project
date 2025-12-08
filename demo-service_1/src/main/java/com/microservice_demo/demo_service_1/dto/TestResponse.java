package com.microservice_demo.demo_service_1.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {

    private String status;
    private String message;
    private String service;
    private String username;
    private String roles;
    private LocalDateTime timeStamp;

}
