package com.example.autosalon.mapper;

import com.example.autosalon.dto.DealershipRequestDto;
import com.example.autosalon.dto.DealershipResponseDto;
import com.example.autosalon.dto.DealershipWithCarsResponseDto;
import com.example.autosalon.entity.Dealership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DealershipMapper {

    private final CarMapper carMapper;

    public Dealership toEntity(DealershipRequestDto dto) {
        Dealership dealership = new Dealership();
        dealership.setName(dto.getName());
        dealership.setAddress(dto.getAddress());
        dealership.setPhone(dto.getPhone());
        return dealership;
    }

    public DealershipResponseDto toResponseDto(Dealership dealership) {
        DealershipResponseDto dto = new DealershipResponseDto();
        dto.setId(dealership.getId());
        dto.setName(dealership.getName());
        dto.setAddress(dealership.getAddress());
        dto.setPhone(dealership.getPhone());
        return dto;
    }

    public DealershipWithCarsResponseDto toWithCarsResponseDto(Dealership dealership) {
        DealershipWithCarsResponseDto dto = new DealershipWithCarsResponseDto();
        dto.setId(dealership.getId());
        dto.setName(dealership.getName());
        dto.setAddress(dealership.getAddress());
        dto.setPhone(dealership.getPhone());
        dto.setCars(dealership.getCars().stream().map(carMapper::toResponseDto).toList());
        return dto;
    }
}
