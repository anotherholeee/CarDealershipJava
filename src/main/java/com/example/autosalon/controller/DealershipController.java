package com.example.autosalon.controller;

import com.example.autosalon.dto.DealershipWithCarsRequest;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.service.DealershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/dealerships")
@RequiredArgsConstructor
public class DealershipController {

    private final DealershipService dealershipService;

    /**
     * Демонстрация БЕЗ транзакции (частичное сохранение)
     * POST /api/dealerships/without-transaction
     */
    @PostMapping("/without-transaction")
    public String createWithoutTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info("\n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/without-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info("📊 До операции: салонов в БД = {}", beforeCount);

        try {
            Dealership saved = dealershipService.createDealershipWithCarsWithoutTransaction(
                    request.getDealership(),
                    request.getCars()
            );

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    "✅ УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. Салон '%s' сохранен!",
                    beforeCount, afterCount, saved.getName()
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error("Ошибка при сохранении: {}", e.getMessage());
            return String.format(
                    "❌ ОШИБКА: %s%n📊 Салонов было: %d, стало: %d. Видите? Салон сохранился, хотя должна была быть ошибка! Это частичное сохранение.",
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
        log.info("📊 До операции: салонов в БД = {}", beforeCount);

        try {
            Dealership saved = dealershipService.createDealershipWithCarsWithTransaction(
                    request.getDealership(),
                    request.getCars()
            );

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    "✅ УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d.",
                    beforeCount, afterCount
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error("Ошибка при сохранении, транзакция откатилась: {}", e.getMessage());
            return String.format(
                    "✅ ОТКАТ: %s%n📊 Салонов было: %d, стало: %d. Отлично! Транзакция сработала - салон НЕ сохранился!",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }
}