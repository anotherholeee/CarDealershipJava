package com.example.autosalon.controller;

import com.example.autosalon.dto.*;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.enums.AccountType;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.service.AuthService;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final AuthService authService;

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

    @GetMapping("/mine")
    @Operation(summary = "Получить мои автомобили")
    public ResponseEntity<List<CarResponseDto>> getMyCars(
            @RequestHeader("Authorization") String authorization) {
        UserAccount user = authService.requireUserByToken(authorization);
        return ResponseEntity.ok(carService.getCarsByOwnerAsDtos(user));
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
            @Valid @ModelAttribute CarSearchRequest request) {

        log.info("📄 JPQL С ПАГИНАЦИЕЙ: {}", request);
        PageResponseDto<CarResponseDto> response = carService.findCarsWithPaginationJpql(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Создать автомобиль")
    public ResponseEntity<CarResponseDto> createCar(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CarRequestDto createDto) {
        UserAccount user = authService.requireUserByToken(authorization);
        Car car = carMapper.toEntity(createDto);
        Car savedCar = carService.createCar(car, user, createDto.getOwnerUserId());
        CarResponseDto responseDto = carMapper.toResponseDto(savedCar);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/bulk/dealership")
    @Operation(summary = "Пакетное добавление объявлений (только автосалон)", description = "До 30 объявлений за запрос, все привязываются к текущему пользователю")
    public ResponseEntity<List<CarResponseDto>> createDealershipBulk(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody DealershipCarBulkRequestDto bulkDto) {
        UserAccount user = authService.requireUserByToken(authorization);
        if (user.getAccountType() != AccountType.DEALERSHIP && user.getAccountType() != AccountType.ADMIN) {
            throw new IllegalArgumentException("Пакетное добавление доступно только для автосалона или администратора");
        }
        List<CarResponseDto> result = carService.createDealershipCarBulk(user, bulkDto.getCars());
        return new ResponseEntity<>(result, HttpStatus.CREATED);
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
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody CarRequestDto updateDto) {
        UserAccount user = authService.requireUserByToken(authorization);
        Car carDetails = carMapper.toEntity(updateDto);
        Car updatedCar = carService.updateCar(id, carDetails, user);
        CarResponseDto responseDto = carMapper.toResponseDto(updatedCar);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить автомобиль")
    public ResponseEntity<Void> deleteCar(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        UserAccount user = authService.requireUserByToken(authorization);
        carService.deleteCar(id, user);
        return ResponseEntity.noContent().build();
    }

}