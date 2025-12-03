package com.microservice_demo.demo_service_2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
