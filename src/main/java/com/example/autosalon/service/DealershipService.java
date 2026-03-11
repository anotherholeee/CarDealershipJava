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

    private final DealershipRepository dealershipRepository;
    private final CarRepository carRepository;
    private final CarService carService;
    private final DealershipTransactionalService dealershipTransactionalService;


    @Transactional(readOnly = true)
    public List<Dealership> getAllDealerships() {
        List<Dealership> dealerships = dealershipRepository.findAllWithCars();

        carRepository.findAll();

        for (Dealership d : dealerships) {
            for (Car c : d.getCars()) {
                c.getFeatures().size();
            }
        }

        return dealerships;
    }

    public Dealership getDealershipById(Long id) {
        return dealershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Автосалон с id " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public Dealership getDealershipWithCars(Long id) {
        Dealership dealership = getDealershipById(id);
        if (!dealership.getCars().isEmpty()) {
            log.debug(
                    "Автосалон {} содержит {} машин",
                    dealership.getName(),
                    dealership.getCars().size());
            for (Car car : dealership.getCars()) {
                car.getFeatures().size();
            }
        }
        return dealership;
    }

    @Transactional
    public Dealership createDealership(Dealership dealership) {
        dealership.setId(null);
        return dealershipRepository.save(dealership);
    }

    @Transactional
    public Dealership updateDealership(Long id, Dealership dealershipDetails) {
        Dealership existing = getDealershipById(id);
        existing.setName(dealershipDetails.getName());
        existing.setAddress(dealershipDetails.getAddress());
        existing.setPhone(dealershipDetails.getPhone());
        return existing;
    }

    @Transactional
    public void deleteDealership(Long id) {
        Dealership dealership = getDealershipById(id);

        // Удаляем все машины автосалона, используя общую бизнес-логику CarService.
        // Если какая-то машина уже продана, CarService.deleteCar выбросит исключение
        // и автосалон не будет удалён, что защищает инвариант "проданную машину нельзя удалить".
        List<Car> cars = dealership.getCars();
        for (Car car : cars) {
            carService.deleteCar(car.getId());
        }

        dealershipRepository.delete(dealership);

        log.info("Автосалон с id {} успешно удален", id);
    }


    @Transactional
    public Dealership addCarToDealership(Long dealershipId, Long carId) {
        Dealership dealership = getDealershipById(dealershipId);
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + carId + " не найдена"));

        dealership.addCar(car);
        dealership.getCars().size();
        return dealership;
    }

    @Transactional
    public Dealership removeCarFromDealership(Long dealershipId, Long carId) {
        Dealership dealership = getDealershipById(dealershipId);
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + carId + " не найдена"));

        dealership.removeCar(car);
        dealership.getCars().size();
        return dealership;
    }


    @Transactional(readOnly = true)
    public long countDealerships() {
        return dealershipRepository.count();
    }

    public Dealership createDealershipWithCarsWithoutTransaction(
            Dealership dealership, List<Car> cars) {
        Dealership savedDealership = dealershipRepository.save(dealership);
        saveCarsWithErrorOnSecond(cars, savedDealership);
        return savedDealership;
    }

    @Transactional
    public Dealership createDealershipWithCarsWithTransaction(
            Dealership dealership, List<Car> cars) {
        return dealershipTransactionalService
                .createDealershipWithCarsWithTransaction(dealership, cars);
    }

    private void saveCarsWithErrorOnSecond(List<Car> cars, Dealership dealership) {
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            car.setDealership(dealership);
            car.setId(null);

            if (i == 1) {
                throw new IllegalArgumentException(
                        String.format("Ошибка сохранения машины: %s %s",
                                car.getBrand(), car.getModel())
                );
            }

            carRepository.save(car);
        }
    }
}