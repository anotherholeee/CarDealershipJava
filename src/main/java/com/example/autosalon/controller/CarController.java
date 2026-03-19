package com.example.autosalon.controller;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.CarSearchRequest;
import com.example.autosalon.dto.PageResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.service.CarService;
import java.util.Comparator;
import java.util.List;
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
public class CarController {

    private final CarService carService;
    private final CarMapper carMapper;

    @GetMapping
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
    public ResponseEntity<CarResponseDto> getCarById(@PathVariable Long id) {
        Car car = carService.getCarById(id);
        CarResponseDto responseDto = carMapper.toResponseDto(car);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * JPQL версия без пагинации
     */
    @GetMapping("/search/jpql")
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

    /**
     * Native версия без пагинации
     */
    @GetMapping("/search/native")
    public ResponseEntity<List<CarResponseDto>> searchCarsByFeatureCategoryNative(
            @RequestParam String category) {

        log.info("🟢 NATIVE: GET /api/cars/search/native?category={}", category);

        List<Car> cars = carService.getCarsByFeatureCategoryNative(category);

        List<CarResponseDto> responseDtos = cars.stream()
                .map(carMapper::toResponseDto)
                .sorted(Comparator.comparing(CarResponseDto::getId))
                .toList();

        return ResponseEntity.ok(responseDtos);
    }

    /**
     * НОВЫЙ ЭНДПОИНТ 1: JPQL с пагинацией
     * Примеры использования:
     * - /api/cars/pagination/jpql?featureCategory=Комфорт&page=0&size=2
     * - /api/cars/pagination/jpql?
     * featureCategory=Безопасность&page=1&size=3&sortBy=price&sortDirection=DESC
     * - /api/cars/pagination/jpql?page=0&size=5 (без фильтрации)
     */
    @GetMapping("/pagination/jpql")
    public ResponseEntity<PageResponseDto<CarResponseDto>> getCarsWithPaginationJpql(
            @ModelAttribute CarSearchRequest request) {

        log.info("📄 JPQL С ПАГИНАЦИЕЙ: {}", request);
        PageResponseDto<CarResponseDto> response = carService.findCarsWithPaginationJpql(request);
        return ResponseEntity.ok(response);
    }

    /**
     * НОВЫЙ ЭНДПОИНТ 2: Native Query с пагинацией
     * Примеры использования:
     * - /api/cars/pagination/native?
     * featureCategory=Комфорт&page=0&size=2
     * - /api/cars/pagination/native?
     * featureCategory=Безопасность&page=1&size=3&sortBy=year&sortDirection=DESC
     */
    @GetMapping("/pagination/native")
    public ResponseEntity<PageResponseDto<CarResponseDto>> getCarsWithPaginationNative(
            @ModelAttribute CarSearchRequest request) {

        log.info(" NATIVE С ПАГИНАЦИЕЙ: {}", request);
        PageResponseDto<CarResponseDto> response = carService.findCarsWithPaginationNative(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CarResponseDto> createCar(@RequestBody CarRequestDto createDto) {
        Car car = carMapper.toEntity(createDto);
        Car savedCar = carService.createCar(car);
        CarResponseDto responseDto = carMapper.toResponseDto(savedCar);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponseDto> updateCar(
            @PathVariable Long id,
            @RequestBody CarRequestDto updateDto) {
        Car carDetails = carMapper.toEntity(updateDto);
        Car updatedCar = carService.updateCar(id, carDetails);
        CarResponseDto responseDto = carMapper.toResponseDto(updatedCar);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Демонстрация решения с @EntityGraph
     */
    @GetMapping("/features/solution")
    public ResponseEntity<List<Car>> demonstrateSolution() {
        List<Car> cars = carService.getCarsWithSolution();
        cars.sort(Comparator.comparing(Car::getId));
        return ResponseEntity.ok(cars);
    }
}