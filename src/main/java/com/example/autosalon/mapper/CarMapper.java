package com.example.autosalon.mapper;

import com.example.autosalon.dto.CarImageInfoDto;
import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.CarImage;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.util.CarImagePublicPaths;
import com.example.autosalon.repository.FeatureRepository;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        dto.setInteriorColor(car.getInteriorColor());
        dto.setInteriorMaterial(car.getInteriorMaterial());
        dto.setEngineVolume(car.getEngineVolume());
        dto.setMileage(car.getMileage());
        dto.setPowerHp(car.getPowerHp());
        dto.setFuelConsumptionCity(car.getFuelConsumptionCity());
        dto.setFuelConsumptionHighway(car.getFuelConsumptionHighway());
        dto.setFuelConsumptionMixed(car.getFuelConsumptionMixed());
        dto.setSeatCount(car.getSeatCount());
        dto.setCity(car.getCity());
        dto.setTransmission(car.getTransmission());
        dto.setBodyType(car.getBodyType());
        dto.setEngineType(car.getEngineType());
        dto.setDriveType(car.getDriveType());
        dto.setPrice(car.getPrice());
        String pc = car.getPriceCurrency();
        dto.setPriceCurrency(pc != null && !pc.isBlank() ? pc.trim().toUpperCase() : "USD");
        dto.setFeatureNames(car.getFeatures().stream().map(Feature::getName).toList());
        if (car.getOwner() != null && car.getOwner().getAccountType() != null) {
            dto.setSellerAccountType(car.getOwner().getAccountType().name());
        } else {
            dto.setSellerAccountType(null);
        }
        dto.setSellerDisplayName(resolveSellerDisplayName(car));
        dto.setSellerPhone(resolveSellerPhone(car));
        if (car.getImages() != null && !car.getImages().isEmpty()) {
            /* JOIN FETCH features+images давал дубликаты в списке — уникальность по id, порядок по sortOrder */
            dto.setPhotos(car.getImages().stream()
                    .collect(Collectors.toMap(
                            CarImage::getId,
                            Function.identity(),
                            (a, b) -> a,
                            LinkedHashMap::new))
                    .values()
                    .stream()
                    .sorted(Comparator.comparingInt(CarImage::getSortOrder))
                    .map(img -> new CarImageInfoDto(
                            img.getId(),
                            CarImagePublicPaths.urlPath(car.getId(), img.getFileName())))
                    .toList());
        } else {
            dto.setPhotos(Collections.emptyList());
        }
        dto.setPublishedAt(car.getPublishedAt());
        return dto;
    }

    private static String resolveSellerDisplayName(Car car) {
        Dealership d = car.getDealership();
        if (d != null && d.getName() != null && !d.getName().isBlank()) {
            return d.getName().trim();
        }
        UserAccount o = car.getOwner();
        if (o == null) {
            return null;
        }
        if (o.getCompanyName() != null && !o.getCompanyName().isBlank()) {
            return o.getCompanyName().trim();
        }
        if (o.getPersonName() != null && !o.getPersonName().isBlank()) {
            return o.getPersonName().trim();
        }
        return o.getUsername();
    }

    private static String resolveSellerPhone(Car car) {
        Dealership d = car.getDealership();
        if (d != null && d.getPhone() != null && !d.getPhone().isBlank()) {
            return d.getPhone().trim();
        }
        UserAccount o = car.getOwner();
        if (o == null) {
            return null;
        }
        if (o.getPhone() != null && !o.getPhone().isBlank()) {
            return o.getPhone().trim();
        }
        return null;
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
        car.setInteriorColor(blankToNull(dto.getInteriorColor()));
        car.setInteriorMaterial(blankToNull(dto.getInteriorMaterial()));
        car.setEngineVolume(dto.getEngineVolume());
        car.setMileage(dto.getMileage());
        car.setPowerHp(dto.getPowerHp());
        car.setFuelConsumptionCity(dto.getFuelConsumptionCity());
        car.setFuelConsumptionHighway(dto.getFuelConsumptionHighway());
        car.setFuelConsumptionMixed(dto.getFuelConsumptionMixed());
        car.setSeatCount(dto.getSeatCount());
        car.setCity(dto.getCity() != null ? dto.getCity().trim() : null);
        car.setTransmission(trimCode(dto.getTransmission()));
        car.setBodyType(trimCode(dto.getBodyType()));
        car.setEngineType(trimCode(dto.getEngineType()));
        car.setDriveType(trimCode(dto.getDriveType()));
        car.setPrice(dto.getPrice());
        String cur = dto.getPriceCurrency();
        if (cur == null || cur.isBlank()) {
            car.setPriceCurrency("USD");
        } else {
            car.setPriceCurrency(cur.trim().toUpperCase());
        }

        if (dto.getFeatureIds() != null && !dto.getFeatureIds().isEmpty()) {
            car.setFeatures(new LinkedHashSet<>(featureRepository.findAllById(dto.getFeatureIds())));
        }

        return car;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String trimCode(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}