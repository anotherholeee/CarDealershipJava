package com.example.autosalon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarResponseDto {
    private Long id;
    private String brand;
    private String model;
    private int year;
    private String color;
    private double price;
}