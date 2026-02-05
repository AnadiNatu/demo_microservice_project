package com.microservice_demo.demo_service_1.dto.functionality;

//import jakarta.validation.constraints.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductDto {

    @NotBlank
    @Size(max = 200)
    private String productName;

    @Size(max = 1000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull
    @Min(value = 0)
    private Integer stockQuantity;

    @Size(max = 100)
    private String category;

    @Size(max = 50)
    private String sku;

    private Long createdByUserId;
}
