package com.microservice_demo.demo_service_2.dto.functionality;

import jakarta.validation.Valid;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import jakarta.validation.constraints.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatedOrderDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "User ID is required.")
    private Long userId;

    @Valid
    @NotEmpty(message = "Order must contain at least one product.")
    private List<CreateOrderItemDto> items;

    @NotBlank(message = "Shipping name is required.")
    @Size(max = 100)
    private String shippingName;

    @NotBlank(message = "Shipping phone is required.")
    @Size(max = 20)
    private String shippingPhone;

    @Email(message = "Invalid email address.")
    @NotBlank(message = "Shipping email is required.")
    @Size(max = 150)
    private String shippingEmail;

    @NotBlank(message = "Shipping address is required.")
    @Size(max = 500)
    private String shippingAddress;

    @NotBlank(message = "Shipping city is required.")
    @Size(max = 100)
    private String shippingCity;

    @NotBlank(message = "Shipping state is required.")
    @Size(max = 100)
    private String shippingState;

    @NotBlank(message = "Shipping country is required.")
    @Size(max = 100)
    private String shippingCountry;

    @NotBlank(message = "Postal code is required.")
    @Size(max = 20)
    private String postalCode;

    @Size(max = 1000)
    private String notes;

    private Long CreatedByUserId;
}