package com.microservice_demo.demo_service_2.dto.functionality;

import lombok.*;
import java.util.List;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatedOrderDto {

    @NotNull
    private Long userId;

    @NotEmpty
    private List<Long> productIds;

    @NotEmpty(message = "Quantities are required")
    private List<Integer> quantities;

    @NotBlank
    @Size(max = 500)
    private String shippingAddress;

    @Size(max = 1000)
    private String notes;
}