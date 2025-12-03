package com.microservice_demo.demo_service_1.dto;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoEntity1Dto {

    private Long demoEn1Id;
    private String demoData;
    private Date createdOn;
    private Date updatedOn;

}
