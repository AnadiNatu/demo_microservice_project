package com.microservice_demo.demo_service_1.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDemoEntity1Dto {
    private String demoData;
    private Long userId;
}
