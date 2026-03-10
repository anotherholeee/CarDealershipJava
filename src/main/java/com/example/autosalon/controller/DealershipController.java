package com.example.autosalon.controller;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.DealershipRequestDto;
import com.example.autosalon.dto.DealershipResponseDto;
import com.example.autosalon.dto.DealershipWithCarsRequest;
import com.example.autosalon.dto.DealershipWithCarsResponseDto;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.mapper.DealershipMapper;
import com.example.autosalon.service.DealershipService;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RestController
@RequestMapping("/api/dealerships")
@RequiredArgsConstructor
public class DealershipController {

    private final DealershipService dealershipService;
    private final DealershipMapper dealershipMapper;
    private final CarMapper carMapper;

    /**
     * Получить все автосалоны
     * GET /api/dealerships
     */
    @GetMapping
    public ResponseEntity<List<DealershipResponseDto>> getAllDealerships() {
        log.info("GET /api/dealerships - получение всех автосалонов");
        List<Dealership> dealerships = dealershipService.getAllDealerships();


        List<DealershipResponseDto> response = dealerships.stream()
                .sorted(Comparator.comparing(Dealership::getId))
                .map(dealershipMapper::toResponseDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Получить автосалон по ID
     * GET /api/dealerships/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DealershipResponseDto> getDealershipById(@PathVariable Long id) {
        log.info("GET /api/dealerships/{} - получение автосалона", id);
        Dealership dealership = dealershipService.getDealershipById(id);
        return ResponseEntity.ok(dealershipMapper.toResponseDto(dealership));
    }

    /**
     * Получить автосалон с машинами
     * GET /api/dealerships/{id}/with-cars
     */
    @GetMapping("/{id}/with-cars")
    public ResponseEntity<DealershipWithCarsResponseDto> getDealershipWithCars(
            @PathVariable Long id) {
        log.info(
                "GET /api/dealerships/{}/with-cars - получение салона с машинами",
                id);
        Dealership dealership = dealershipService.getDealershipWithCars(id);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }

    /**
     * Создать новый автосалон
     * POST /api/dealerships
     */
    @PostMapping
    public ResponseEntity<DealershipResponseDto> createDealership(
            @RequestBody DealershipRequestDto requestDto) {
        log.info(
                "POST /api/dealerships - создание нового автосалона: {}",
                requestDto.getName());
        Dealership created = dealershipService.createDealership(
                dealershipMapper.toEntity(requestDto));
        return new ResponseEntity<>(
                dealershipMapper.toResponseDto(created),
                HttpStatus.CREATED);
    }

    /**
     * Полностью обновить автосалон
     * PUT /api/dealerships/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DealershipResponseDto> updateDealership(
            @PathVariable Long id,
            @RequestBody DealershipRequestDto requestDto) {
        log.info("PUT /api/dealerships/{} - обновление автосалона", id);
        Dealership updated = dealershipService.updateDealership(
                id,
                dealershipMapper.toEntity(requestDto));
        return ResponseEntity.ok(dealershipMapper.toResponseDto(updated));
    }

    /**
     * УДАЛИТЬ автосалон по ID
     * DELETE /api/dealerships/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDealership(@PathVariable Long id) {
        log.info(
                "DELETE /api/dealerships/{} - удаление автосалона",
                id);
        dealershipService.deleteDealership(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Добавить машину в автосалон
     * POST /api/dealerships/{dealershipId}/cars/{carId}
     */
    @PostMapping("/{dealershipId}/cars/{carId}")
    public ResponseEntity<DealershipWithCarsResponseDto> addCarToDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info(
                "POST /api/dealerships/{}/cars/{} - добавление машины в салон",
                dealershipId,
                carId);
        Dealership dealership = dealershipService.addCarToDealership(
                dealershipId,
                carId);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }

    /**
     * УДАЛИТЬ машину из автосалона
     * DELETE /api/dealerships/{dealershipId}/cars/{carId}
     */
    @DeleteMapping("/{dealershipId}/cars/{carId}")
    public ResponseEntity<DealershipWithCarsResponseDto> removeCarFromDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info(
                "DELETE /api/dealerships/{}/cars/{} - удаление машины из салона",
                dealershipId,
                carId);
        Dealership dealership = dealershipService.removeCarFromDealership(
                dealershipId,
                carId);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }



    /**
     * Демонстрация БЕЗ транзакции (частичное сохранение)
     * POST /api/dealerships/without-transaction
     */
    @PostMapping("/without-transaction")
    public String createWithoutTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info(
                "\n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/without-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info(" До операции: салонов в БД = {}", beforeCount);

        try {
            List<CarRequestDto> cars = request.getCars() == null
                    ? Collections.emptyList()
                    : request.getCars();
            Dealership saved =
                    dealershipService.createDealershipWithCarsWithoutTransaction(
                            dealershipMapper.toEntity(request.getDealership()),
                            cars.stream()
                                    .map(carMapper::toEntity)
                                    .toList());

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    " УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. "
                            + "Салон '%s' сохранен! (Проблема: машины не сохранились, "
                            + "но салон остался)",
                    beforeCount, afterCount, saved.getName()
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error(
                    "Ошибка при сохранении: {}",
                    e.getMessage());
            return String.format(
                    "️ ОШИБКА: %s%n Салонов было: %d, стало: %d. "
                            + "(Данные сохранились частично - салон остался в БД!)",
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
        log.info(
                "\n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/with-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info(" До операции: салонов в БД = {}", beforeCount);

        try {
            List<CarRequestDto> cars = request.getCars() == null
                    ? Collections.emptyList()
                    : request.getCars();
            dealershipService.createDealershipWithCarsWithTransaction(
                    dealershipMapper.toEntity(request.getDealership()),
                    cars.stream()
                            .map(carMapper::toEntity)
                            .toList());

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    " УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. "
                            + "(Этого не должно произойти с @Transactional!)",
                    beforeCount, afterCount
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error(
                    "Ошибка при сохранении, транзакция откатилась: {}",
                    e.getMessage());
            return String.format(
                    " ОТКАТ: %s%n Салонов было: %d, стало: %d. "
                            + "Отлично! Транзакция сработала - салон НЕ сохранился!",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }
}