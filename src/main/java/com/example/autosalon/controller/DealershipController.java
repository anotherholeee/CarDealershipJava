package com.example.autosalon.controller;

import com.example.autosalon.dto.DealershipWithCarsRequest;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.service.DealershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dealerships")
@RequiredArgsConstructor
public class DealershipController {

    private final DealershipService dealershipService;



    /**
     * Получить все автосалоны
     * GET /api/dealerships
     */
    @GetMapping
    public ResponseEntity<List<Dealership>> getAllDealerships() {
        log.info("GET /api/dealerships - получение всех автосалонов");
        List<Dealership> dealerships = dealershipService.getAllDealerships();


        dealerships.sort(Comparator.comparing(Dealership::getId));

        return ResponseEntity.ok(dealerships);
    }

    /**
     * Получить автосалон по ID
     * GET /api/dealerships/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Dealership> getDealershipById(@PathVariable Long id) {
        log.info("GET /api/dealerships/{} - получение автосалона", id);
        return ResponseEntity.ok(dealershipService.getDealershipById(id));
    }

    /**
     * Получить автосалон с машинами
     * GET /api/dealerships/{id}/with-cars
     */
    @GetMapping("/{id}/with-cars")
    public ResponseEntity<Dealership> getDealershipWithCars(@PathVariable Long id) {
        log.info("GET /api/dealerships/{}/with-cars - получение салона с машинами", id);
        return ResponseEntity.ok(dealershipService.getDealershipWithCars(id));
    }

    /**
     * Создать новый автосалон
     * POST /api/dealerships
     */
    @PostMapping
    public ResponseEntity<Dealership> createDealership(@RequestBody Dealership dealership) {
        log.info("POST /api/dealerships - создание нового автосалона: {}", dealership.getName());
        Dealership created = dealershipService.createDealership(dealership);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Полностью обновить автосалон
     * PUT /api/dealerships/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Dealership> updateDealership(
            @PathVariable Long id,
            @RequestBody Dealership dealership) {
        log.info("PUT /api/dealerships/{} - обновление автосалона", id);
        return ResponseEntity.ok(dealershipService.updateDealership(id, dealership));
    }

    /**
     * УДАЛИТЬ автосалон по ID
     * DELETE /api/dealerships/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDealership(@PathVariable Long id) {
        log.info("DELETE /api/dealerships/{} - удаление автосалона", id);
        dealershipService.deleteDealership(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Добавить машину в автосалон
     * POST /api/dealerships/{dealershipId}/cars/{carId}
     */
    @PostMapping("/{dealershipId}/cars/{carId}")
    public ResponseEntity<Dealership> addCarToDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info("POST /api/dealerships/{}/cars/{} - добавление машины в салон", dealershipId, carId);
        return ResponseEntity.ok(dealershipService.addCarToDealership(dealershipId, carId));
    }

    /**
     * УДАЛИТЬ машину из автосалона
     * DELETE /api/dealerships/{dealershipId}/cars/{carId}
     */
    @DeleteMapping("/{dealershipId}/cars/{carId}")
    public ResponseEntity<Dealership> removeCarFromDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info("DELETE /api/dealerships/{}/cars/{} - удаление машины из салона", dealershipId, carId);
        return ResponseEntity.ok(dealershipService.removeCarFromDealership(dealershipId, carId));
    }



    /**
     * Демонстрация БЕЗ транзакции (частичное сохранение)
     * POST /api/dealerships/without-transaction
     */
    @PostMapping("/without-transaction")
    public String createWithoutTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info("\n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/without-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info(" До операции: салонов в БД = {}", beforeCount);

        try {
            Dealership saved = dealershipService
                    .createDealershipWithCarsWithoutTransaction(
                            request.getDealership(),
                            request.getCars()
                    );

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    " УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. " +
                            "Салон '%s' сохранен! (Проблема: машины не сохранились, но салон остался)",
                    beforeCount, afterCount, saved.getName()
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error("Ошибка при сохранении: {}", e.getMessage());
            return String.format(
                    "️ ОШИБКА: %s%n Салонов было: %d, стало: %d. " +
                            "(Данные сохранились частично - салон остался в БД!)",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }

    /**
     * Демонстрация С транзакцией (полный откат)
     * POST /api/dealerships/with-transaction
     */
    @PostMapping("/with-transaction")
    public String createWithTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info("\n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/with-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info(" До операции: салонов в БД = {}", beforeCount);

        try {
            dealershipService.createDealershipWithCarsWithTransaction(
                    request.getDealership(),
                    request.getCars()
            );

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    " УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. " +
                            "(Этого не должно произойти с @Transactional!)",
                    beforeCount, afterCount
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error("Ошибка при сохранении, транзакция откатилась: {}", e.getMessage());
            return String.format(
                    " ОТКАТ: %s%n Салонов было: %d, стало: %d. " +
                            "Отлично! Транзакция сработала - салон НЕ сохранился!",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }
}