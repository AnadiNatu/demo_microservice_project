package com.microservice_demo.demo_service_2.dto.functionality;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderItemDto {

    @NotNull(message = "Product ID is required.")
    private Long productId;

    @NotNull
    @Min(value = 1)
    private Integer quantity;

}

