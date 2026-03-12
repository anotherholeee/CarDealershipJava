package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.CarRepositoryWithoutGraph;
import com.example.autosalon.repository.SaleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarRepositoryWithoutGraph carRepositoryWithout;
    private final SaleRepository saleRepository;
    private final ObjectProvider<CarService> self;

    @Transactional(readOnly = true)
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + id + " не найдена"));
    }

    @Transactional
    public Car createCar(Car car) {
        car.setId(null);
        return carRepository.save(car);
    }

    @Transactional
    public Car updateCar(Long id, Car carDetails) {
        Car existingCar = self.getObject().getCarById(id);

        existingCar.setBrand(carDetails.getBrand());
        existingCar.setModel(carDetails.getModel());
        existingCar.setYear(carDetails.getYear());
        existingCar.setColor(carDetails.getColor());
        existingCar.setPrice(carDetails.getPrice());

        return existingCar;
    }

    @Transactional
    public void deleteCar(Long id) {
        Car car = self.getObject().getCarById(id);

        if (car.getSale() != null) {
            Sale sale = car.getSale();
            String errorMessage = String.format(
                    "Невозможно удалить машину ID=%d %s %s %d - она уже продана! "
                            +
                            "(ID продажи: %d, дата продажи: %s, покупатель: %s %s)",
                    car.getId(),
                    car.getBrand(),
                    car.getModel(),
                    car.getYear(),
                    sale.getId(),
                    sale.getSaleDate().toLocalDate(),
                    sale.getCustomer() != null ? sale.getCustomer().getFirstName() : "неизвестно",
                    sale.getCustomer() != null ? sale.getCustomer().getLastName() : "неизвестно"
            );

            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("Удаление машины ID={} {} {} (не продана)",
                car.getId(), car.getBrand(), car.getModel());

        car.getFeatures().clear();
        carRepository.delete(car);
        log.info("Машина с id {} успешно удалена", id);
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByBrand(String brand) {
        return carRepository.findByBrandIgnoreCase(brand);
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsWithNplusOneProblem() {
        log.info("=== ПРОБЛЕМА N+1: обычный findAll ===");
        return carRepositoryWithout.findAll();
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsWithSolution() {
        log.info("=== РЕШЕНИЕ: findAll с @EntityGraph ===");
        return carRepository.findAll();
    }
}