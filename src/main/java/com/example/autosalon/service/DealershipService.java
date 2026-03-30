package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealershipService {
    static final String DEALERSHIP_NOT_FOUND_MESSAGE = "Автосалон с id %d не найден";

    private final DealershipRepository dealershipRepository;
    private final CarRepository carRepository;
    private final CarService carService;
    private final DealershipTransactionalService dealershipTransactionalService;

    @Transactional(readOnly = true)
    public List<Dealership> getAllDealerships() {
        return dealershipRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Dealership getDealershipById(Long id) {
        return dealershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(DEALERSHIP_NOT_FOUND_MESSAGE, id)));
    }

    @Transactional(readOnly = true)
    public Dealership getDealershipWithCars(Long id) {
        return dealershipRepository.findByIdWithCars(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(DEALERSHIP_NOT_FOUND_MESSAGE, id)));
    }

    @Transactional
    public Dealership createDealership(Dealership dealership) {
        dealership.setId(null);
        return dealershipRepository.save(dealership);
    }

    @Transactional
    public Dealership updateDealership(Long id, Dealership dealershipDetails) {
        Dealership existing = dealershipTransactionalService.getDealershipById(id);
        existing.setName(dealershipDetails.getName());
        existing.setAddress(dealershipDetails.getAddress());
        existing.setPhone(dealershipDetails.getPhone());
        return existing;
    }

    @Transactional
    public void deleteDealership(Long id) {
        Dealership dealership = dealershipRepository.findByIdWithCars(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(DEALERSHIP_NOT_FOUND_MESSAGE, id)));

        List<Car> cars = dealership.getCars();
        for (Car car : cars) {
            carService.deleteCar(car.getId());
        }

        dealershipRepository.delete(dealership);

        log.info("Автосалон с id {} успешно удален", id);
    }

    @Transactional
    public Dealership addCarToDealership(Long dealershipId, Long carId) {
        Dealership dealership = dealershipRepository.findByIdWithCars(dealershipId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(DEALERSHIP_NOT_FOUND_MESSAGE, dealershipId)));
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + carId + " не найдена"));

        dealership.addCar(car);
        return dealership;
    }

    @Transactional
    public Dealership removeCarFromDealership(Long dealershipId, Long carId) {
        Dealership dealership = dealershipRepository.findByIdWithCars(dealershipId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(DEALERSHIP_NOT_FOUND_MESSAGE, dealershipId)));
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + carId + " не найдена"));

        dealership.removeCar(car);
        return dealership;
    }

    @Transactional(readOnly = true)
    public long countDealerships() {
        return dealershipRepository.count();
    }

}