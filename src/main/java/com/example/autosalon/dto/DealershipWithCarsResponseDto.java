package com.example.autosalon.dto;

import java.util.List;
import lombok.Data;

@Data
public class DealershipWithCarsResponseDto {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private List<CarResponseDto> cars;
}

