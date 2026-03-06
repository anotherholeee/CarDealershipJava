package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarRepositoryWithoutGraph extends JpaRepository<Car, Long> {
    List<Car> findByBrandIgnoreCase(String brand);
}