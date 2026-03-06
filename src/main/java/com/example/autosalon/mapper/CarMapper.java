package com.example.autosalon.mapper;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.repository.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CarMapper {

    private final FeatureRepository featureRepository;

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

        if (dto.getFeatureIds() != null && !dto.getFeatureIds().isEmpty()) {
            List<Feature> features = featureRepository.findAllById(dto.getFeatureIds());
            car.setFeatures(features);
        }

        return car;
    }
}