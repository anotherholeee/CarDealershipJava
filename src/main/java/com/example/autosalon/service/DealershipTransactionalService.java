package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DealershipTransactionalService {

    private final DealershipRepository dealershipRepository;
    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public Dealership getDealershipById(Long id) {
        return dealershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(DealershipService.DEALERSHIP_NOT_FOUND_MESSAGE, id)));
    }

    @Transactional
    public Dealership createDealershipWithCarsWithTransaction(
            Dealership dealership,
            List<Car> cars) {
        Dealership savedDealership = dealershipRepository.save(dealership);
        saveCarsWithErrorOnSecond(cars, savedDealership);
        return savedDealership;
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

