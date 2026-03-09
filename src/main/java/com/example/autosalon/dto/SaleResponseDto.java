package com.example.autosalon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponseDto {
    private Long id;
    private Long carId;
    private String carBrand;
    private String carModel;
    private Long customerId;
    private String customerName;
    private LocalDateTime saleDate;
    private double salePrice;
}