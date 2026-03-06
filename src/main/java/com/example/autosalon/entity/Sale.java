package com.example.autosalon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность "Продажа"
 * Таблица: sales
 *
 * Связи:
 * 1. @OneToOne с Car - одна продажа относится к одной машине (владелец)
 * 2. @ManyToOne с Customer - много продаж могут быть у одного покупателя
 */
@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "sale_price")
    private double salePrice;
}