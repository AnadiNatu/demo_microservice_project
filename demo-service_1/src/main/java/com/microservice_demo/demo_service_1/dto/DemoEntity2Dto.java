package com.microservice_demo.demo_service_1.dto;

import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoEntity2Dto {

    private Long demoEn2Id;
    private String demoInfo;
    private String entityStatus;
    private int countField;
    private double priceField;
    private List<String> userName;
    private List<Long> userId;
    private Long de1Id;

}
