package com.example.autosalon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

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