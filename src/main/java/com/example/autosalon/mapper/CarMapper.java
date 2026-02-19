package com.example.autosalon.mapper;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.entity.Car;
import org.springframework.stereotype.Component;

@Component
public class CarMapper {

    public CarResponseDto toResponseDto(Car car) {
        if (car == null) {
            return null;
        }
        CarResponseDto dto = new CarResponseDto();
        dto.setId(car.getId());
        dto.setBrand(car.getBrand());
        dto.setModel(car.getModel());
        dto.setYear(car.getYear());
        dto.setColor(car.getColor());
        dto.setPrice(car.getPrice());
        return dto;
    }

    public Car toEntity(CarRequestDto dto) {
        if (dto == null) {
            return null;
        }
        Car car = new Car();
        car.setBrand(dto.getBrand());
        car.setModel(dto.getModel());
        car.setYear(dto.getYear());
        car.setColor(dto.getColor());
        car.setPrice(dto.getPrice());
        return car;
    }
}