package com.example.autosalon.controller;

import com.example.autosalon.dto.*;
import com.example.autosalon.entity.Car;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Tag(name = "Cars", description = "Операции с автомобилями")
public class CarController {

    private final CarService carService;
    private final CarMapper carMapper;

    @GetMapping
    @Operation(summary = "Получить список автомобилей", description = "Возвращает все автомобили или фильтрует по бренду")
    public ResponseEntity<List<CarResponseDto>> getCars(
            @RequestParam(required = false) String brand) {
        List<Car> cars;
        if (brand != null) {
            cars = carService.getCarsByBrand(brand);
        } else {
            cars = carService.getAllCars();
        }

        List<CarResponseDto> responseDtos = cars.stream()
                .map(carMapper::toResponseDto)
                .sorted(Comparator.comparing(CarResponseDto::getId))
                .toList();

        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить автомобиль по ID")
    public ResponseEntity<CarResponseDto> getCarById(@PathVariable Long id) {
        Car car = carService.getCarById(id);
        CarResponseDto responseDto = carMapper.toResponseDto(car);
        return ResponseEntity.ok(responseDto);
    }


    @GetMapping("/search/jpql")
    @Operation(summary = "Поиск автомобилей по категории опции (JPQL)")
    public ResponseEntity<List<CarResponseDto>> getCarsByFeatureCategoryJpql(
            @RequestParam String category) {

        log.info("🔵 JPQL: GET /api/cars/search/jpql?category={}", category);

        List<Car> cars = carService.getCarsByFeatureCategoryJpql(category);

        List<CarResponseDto> responseDtos = cars.stream()
                .map(carMapper::toResponseDto)
                .sorted(Comparator.comparing(CarResponseDto::getId))
                .toList();

        return ResponseEntity.ok(responseDtos);
    }


    @GetMapping("/pagination/jpql")
    @Operation(summary = "Поиск автомобилей с пагинацией (JPQL)")
    public ResponseEntity<PageResponseDto<CarResponseDto>> getCarsWithPaginationJpql(
            @ModelAttribute CarSearchRequest request) {

        log.info("📄 JPQL С ПАГИНАЦИЕЙ: {}", request);
        PageResponseDto<CarResponseDto> response = carService.findCarsWithPaginationJpql(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Создать автомобиль")
    public ResponseEntity<CarResponseDto> createCar(@Valid @RequestBody CarRequestDto createDto) {
        Car car = carMapper.toEntity(createDto);
        Car savedCar = carService.createCar(car);
        CarResponseDto responseDto = carMapper.toResponseDto(savedCar);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/bulk/transactional")
    @Operation(summary = "Массовое создание с транзакцией (откат при ошибке)")
    public ResponseEntity<List<CarResponseDto>> createCarsBulkTransactional(
            @Valid @RequestBody CarListRequestDto bulkDto) {
        List<CarResponseDto> result = carService.createCarsBulkTransactional(bulkDto.getCars());
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/bulk/non-transactional")
    @Operation(summary = "Массовое создание без транзакции (частичное сохранение)")
    public ResponseEntity<List<CarResponseDto>> createCarsBulkNonTransactional(
            @Valid @RequestBody CarListRequestDto bulkDto) {
        List<CarResponseDto> result = carService.createCarsBulkNonTransactional(bulkDto.getCars());
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить автомобиль")
    public ResponseEntity<CarResponseDto> updateCar(
            @PathVariable Long id,
            @Valid @RequestBody CarRequestDto updateDto) {
        Car carDetails = carMapper.toEntity(updateDto);
        Car updatedCar = carService.updateCar(id, carDetails);
        CarResponseDto responseDto = carMapper.toResponseDto(updatedCar);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить автомобиль")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }

}