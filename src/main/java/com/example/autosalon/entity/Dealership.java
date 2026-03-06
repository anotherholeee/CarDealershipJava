package com.example.autosalon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Автосалон"
 * Таблица: dealerships
 *
 * Связи:
 * 1. @OneToMany с Car - один автосалон имеет много машин
 */
@Entity
@Table(name = "dealerships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cars")
public class Dealership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;


    @OneToMany(mappedBy = "dealership", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Car> cars = new ArrayList<>();



    /**
     * Добавляет машину в автосалон
     * Устанавливает связь с двух сторон
     */
    public void addCar(Car car) {
        cars.add(car);
        car.setDealership(this);
    }

    /**
     * Удаляет машину из автосалона
     * Убирает связь с двух сторон
     */
    public void removeCar(Car car) {
        cars.remove(car);
        car.setDealership(null);
    }
}