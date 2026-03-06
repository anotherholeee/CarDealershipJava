package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByBrandIgnoreCase(String brand);

    @Override
    @EntityGraph(attributePaths = {"features", "sale"})
    List<Car> findAll();
}