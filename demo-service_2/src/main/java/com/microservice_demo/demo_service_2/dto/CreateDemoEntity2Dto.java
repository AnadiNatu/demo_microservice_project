package com.microservice_demo.demo_service_2.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDemoEntity2Dto {
    private String demoInfo;
    private String entityStatus;
    private int countField;
    private double priceField;

}
