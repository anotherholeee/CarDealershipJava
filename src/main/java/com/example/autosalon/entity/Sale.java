package com.example.autosalon.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность "Продажа"
 * Таблица: sales
 *
 * Связи:
 * 1. @OneToOne с Car - одна продажа относится к одной машине
 * 2. @ManyToOne с Customer - много продаж могут быть у одного покупателя
 */
@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"car", "customer"})
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", unique = true)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "date", nullable = false)  // ← Используем существующую колонку date
    private LocalDateTime saleDate;  // ← Поле в коде называется saleDate, но в БД date

    @Column(name = "price")  // ← Используем существующую колонку price
    private double salePrice;  // ← Поле в коде называется salePrice, но в БД price
}