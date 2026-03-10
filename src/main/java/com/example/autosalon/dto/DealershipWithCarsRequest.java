package com.example.autosalon.dto;

import java.util.List;
import lombok.Data;

@Data
public class DealershipWithCarsRequest {
    private DealershipRequestDto dealership;
    private List<CarRequestDto> cars;
}

