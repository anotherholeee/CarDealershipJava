package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {


    private final CarRepository carRepository;


    public List<Car> getAllCars() {
        return carRepository.findAll();
    }


    public Car getCarById(Long id) {

        return carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Машина с id " + id + " не найдена"));
    }


    public Car createCar(Car car) {
        return carRepository.save(car);
    }

    public Car updateCar(Long id, Car carDetails) {
        Car existingCar = getCarById(id);

        existingCar.setBrand(carDetails.getBrand());
        existingCar.setModel(carDetails.getModel());
        existingCar.setYear(carDetails.getYear());
        existingCar.setColor(carDetails.getColor());
        existingCar.setPrice(carDetails.getPrice());

        return carRepository.save(existingCar);
    }

    public void deleteCar(Long id) {
        if (carRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Машина с id " + id + " не найдена");
        }
        carRepository.deleteById(id);
    }

    public List<Car> getCarsByBrand(String brand) {
        return carRepository.findByBrand(brand);
    }
}