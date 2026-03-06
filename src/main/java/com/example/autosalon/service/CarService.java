package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.CarRepositoryWithoutGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;                 // обычный репозиторий
    private final CarRepositoryWithoutGraph carRepositoryWithout; // репозиторий без @EntityGraph


    @Transactional(readOnly = true)
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Машина с id " + id + " не найдена"));
    }

    @Transactional
    public Car createCar(Car car) {
        car.setId(null);
        return carRepository.save(car);
    }

    @Transactional
    public Car updateCar(Long id, Car carDetails) {
        Car existingCar = getCarById(id);
        existingCar.setBrand(carDetails.getBrand());
        existingCar.setModel(carDetails.getModel());
        existingCar.setYear(carDetails.getYear());
        existingCar.setColor(carDetails.getColor());
        existingCar.setPrice(carDetails.getPrice());
        return existingCar;
    }

    @Transactional
    public void deleteCar(Long id) {
        if (!carRepository.existsById(id)) {
            throw new IllegalArgumentException("Машина с id " + id + " не найдена");
        }
        carRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByBrand(String brand) {
        return carRepository.findByBrandIgnoreCase(brand);
    }


    @Transactional(readOnly = true)
    public List<Car> getCarsWithNPlusOneProblem() {
        log.info("=== ПРОБЛЕМА N+1: обычный findAll ===");
        return carRepositoryWithout.findAll();  // используем репозиторий БЕЗ @EntityGraph
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsWithSolution() {
        log.info("=== РЕШЕНИЕ: findAll с @EntityGraph ===");
        return carRepository.findAll();  // используем репозиторий С @EntityGraph
    }
}