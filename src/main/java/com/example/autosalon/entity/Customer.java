package com.example.autosalon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Покупатель"
 * Таблица: customers
 *
 * Связи:
 * 1. @OneToMany с Sale - один покупатель может совершить много покупок
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "sales")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;


    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sale> sales = new ArrayList<>();


    /**
     * Добавляет продажу покупателю
     * Устанавливает связь с двух сторон
     */
    public void addSale(Sale sale) {
        sales.add(sale);
        sale.setCustomer(this);
    }

    /**
     * Удаляет продажу у покупателя
     * Убирает связь с двух сторон
     */
    public void removeSale(Sale sale) {
        sales.remove(sale);
        sale.setCustomer(null);
    }
}