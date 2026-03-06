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

    /**
     * Метод БЕЗ @Transactional - при ошибке часть данных сохранится
     * Демонстрация частичного сохранения
     */
    public Dealership createDealershipWithCarsWithoutTransaction(
            Dealership dealership, List<Car> cars) {
        log.info("\n");
        log.info("========== ДЕМОНСТРАЦИЯ БЕЗ @Transactional ==========");
        log.info("Начинаем сохранение автосалона и машин...");

        Dealership savedDealership = dealershipRepository.save(dealership);
        log.info("✅ Шаг 1: Автосалон '{}' сохранен в БД",
                savedDealership.getName());

        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            car.setDealership(savedDealership);

            if (i == 1) {
                log.error("❌ Шаг {}: ОШИБКА при сохранении машины {} {} !!!",
                        i + 2, car.getBrand(), car.getModel());
                throw new IllegalArgumentException(
                        "💥 Ошибка сохранения машины: "
                                + car.getBrand() + " " + car.getModel());
            }

            Car savedCar = carRepository.save(car);
            log.info("✅ Шаг {}: Машина {} {} сохранена",
                    i + 2, savedCar.getBrand(), savedCar.getModel());
        }

        log.info("✅ Все данные успешно сохранены!");
        return savedDealership;
    }

    /**
     * Метод С @Transactional - при ошибке все откатится
     * Демонстрация полного отката
     */
    @Transactional
    public Dealership createDealershipWithCarsWithTransaction(
            Dealership dealership, List<Car> cars) {
        log.info("\n");
        log.info("========== ДЕМОНСТРАЦИЯ С @Transactional ==========");
        log.info("Начинаем сохранение автосалона и машин...");

        Dealership savedDealership = dealershipRepository.save(dealership);
        log.info("✅ Шаг 1: Автосалон '{}' сохранен (в транзакции)",
                savedDealership.getName());

        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            car.setDealership(savedDealership);

            if (i == 1) {
                log.error("❌ Шаг {}: ОШИБКА при сохранении машины {} {} !!!",
                        i + 2, car.getBrand(), car.getModel());
                log.info("🔄 Транзакция откатывается... "
                        + "Все изменения будут отменены!");
                throw new IllegalArgumentException(
                        "💥 Ошибка сохранения машины: "
                                + car.getBrand() + " " + car.getModel());
            }

            Car savedCar = carRepository.save(car);
            log.info("✅ Шаг {}: Машина {} {} сохранена",
                    i + 2, savedCar.getBrand(), savedCar.getModel());
        }

        log.info("✅ Все данные успешно сохранены!");
        return savedDealership;
    }

    /**
     * Метод для проверки количества салонов в БД
     */
    public long countDealerships() {
        return dealershipRepository.count();
    }
}