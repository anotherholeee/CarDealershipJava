package com.example.autosalon.dto;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import java.util.List;
import lombok.Data;

@Data
public class DealershipWithCarsRequest {
    private Dealership dealership;
    private List<Car> cars;
}