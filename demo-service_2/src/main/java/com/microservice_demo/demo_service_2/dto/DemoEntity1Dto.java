package com.microservice_demo.demo_service_2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DemoEntity1Dto {

    private Long demoEn1Id;
    private String demoData;
    private Date createdOn;
    private Date updatedOn;

}
