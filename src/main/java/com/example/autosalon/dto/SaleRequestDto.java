package com.example.autosalon.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDto {
    private Long carId;
    private Long customerId;
    private LocalDateTime saleDate;
    private double salePrice;
}