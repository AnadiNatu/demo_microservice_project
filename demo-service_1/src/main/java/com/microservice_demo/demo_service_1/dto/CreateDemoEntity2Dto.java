package com.microservice_demo.demo_service_1.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDemoEntity2Dto {
    private String demoInfo;
    private String entityStatus;
    private int countField;
    private double priceField;
}
