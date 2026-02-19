package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import java.util.List;
import java.util.Optional;

public interface CarRepository {
    List<Car> findAll();

    Optional<Car> findById(Long id);

    Car save(Car car);

    void deleteById(Long id);

    List<Car> findByBrand(String brand);
}