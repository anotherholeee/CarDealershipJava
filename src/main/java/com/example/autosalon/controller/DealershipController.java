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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dealerships")
@RequiredArgsConstructor
@Tag(name = "Dealerships", description = "Операции с автосалонами")
public class DealershipController {

    private final DealershipService dealershipService;
    private final DealershipMapper dealershipMapper;
    private final CarMapper carMapper;

    /**
     * Получить все автосалоны
     * GET /api/dealerships
     */
    @GetMapping
    @Operation(summary = "Получить список автосалонов")
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
    @Operation(summary = "Получить автосалон по ID")
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
    @Operation(summary = "Получить автосалон вместе с машинами")
    public ResponseEntity<DealershipWithCarsResponseDto> getDealershipWithCars(
            @PathVariable Long id) {
        log.info("GET /api/dealerships/{}/with-cars - получение салона с машинами", id);
        Dealership dealership = dealershipService.getDealershipWithCars(id);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }

    /**
     * Создать новый автосалон
     * POST /api/dealerships
     */
    @PostMapping
    @Operation(summary = "Создать автосалон")
    public ResponseEntity<DealershipResponseDto> createDealership(
            @Valid @RequestBody DealershipRequestDto requestDto) {
        log.info("POST /api/dealerships - создание нового автосалона: {}", requestDto.getName());
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
    @Operation(summary = "Обновить автосалон")
    public ResponseEntity<DealershipResponseDto> updateDealership(
            @PathVariable Long id,
            @Valid @RequestBody DealershipRequestDto requestDto) {
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
    @Operation(summary = "Удалить автосалон")
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
    @Operation(summary = "Добавить автомобиль в автосалон")
    public ResponseEntity<DealershipWithCarsResponseDto> addCarToDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info("POST /api/dealerships/{}/cars/{} - добавление машины в салон", dealershipId, carId);
        Dealership dealership = dealershipService.addCarToDealership(dealershipId, carId);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }

    /**
     * УДАЛИТЬ машину из автосалона
     * DELETE /api/dealerships/{dealershipId}/cars/{carId}
     */
    @DeleteMapping("/{dealershipId}/cars/{carId}")
    @Operation(summary = "Удалить автомобиль из автосалона")
    public ResponseEntity<DealershipWithCarsResponseDto> removeCarFromDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info("DELETE /api/dealerships/{}/cars/{} - удаление машины из салона", dealershipId, carId);
        Dealership dealership = dealershipService.removeCarFromDealership(dealershipId, carId);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }

}