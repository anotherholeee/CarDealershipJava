package com.example.autosalon.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarRequestDto {
    private String brand;
    private String model;
    private int year;
    private String color;
    private double price;
    private List<Long> featureIds;
}