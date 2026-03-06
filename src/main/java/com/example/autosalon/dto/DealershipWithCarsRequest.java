package com.example.autosalon.dto;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import lombok.Data;
import java.util.List;

@Data
public class DealershipWithCarsRequest {
    private Dealership dealership;
    private List<Car> cars;
}