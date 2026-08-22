package com.microservice_demo.demo_service_1.dto.functionality;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductDto {

    private String description;

    private BigDecimal price;

    private Integer stockQuantity;

    private String category;

    private String brand;
}
