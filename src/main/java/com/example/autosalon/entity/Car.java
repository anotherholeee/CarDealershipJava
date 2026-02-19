package com.example.autosalon.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Car {
    private Long id;
    private String brand;       // Марка (Toyota, BMW)
    private String model;       // Модель (Camry, X5)
    private int year;           // Год выпуска
    private String color;       // Цвет
    private double price;       // Цена
}