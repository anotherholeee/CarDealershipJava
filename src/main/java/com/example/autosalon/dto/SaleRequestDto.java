package com.example.autosalon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDto {
    private Long carId;
    private Long customerId;
    private LocalDateTime saleDate;
    private double salePrice;
}