package com.example.autosalon.controller;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.entity.Car;
import com.example.autosalon.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final CarMapper carMapper;

    @GetMapping
    public ResponseEntity<List<CarResponseDto>> getCars(@RequestParam(required = false) String brand) {
        List<Car> cars;
        if (brand != null) {
            cars = carService.getCarsByBrand(brand);
        } else {
            cars = carService.getAllCars();
        }

        List<CarResponseDto> responseDtos = cars.stream()
                .map(carMapper::toResponseDto)
                .toList();

        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarResponseDto> getCarById(@PathVariable Long id) {
        Car car = carService.getCarById(id);
        CarResponseDto responseDto = carMapper.toResponseDto(car);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    public ResponseEntity<CarResponseDto> createCar(@RequestBody CarRequestDto createDto) {
        Car car = carMapper.toEntity(createDto);
        Car savedCar = carService.createCar(car);
        CarResponseDto responseDto = carMapper.toResponseDto(savedCar);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponseDto> updateCar(@PathVariable Long id, @RequestBody CarRequestDto updateDto) {
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
}