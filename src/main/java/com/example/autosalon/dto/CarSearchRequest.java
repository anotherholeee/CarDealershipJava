package com.example.autosalon.dto;

import com.example.autosalon.CarModelYear;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.Sort;


@Data
public class CarSearchRequest {

    private String brand;
    private String model;

    @Min(CarModelYear.MIN)
    @Max(CarModelYear.MAX)
    private Integer yearFrom;

    @Min(CarModelYear.MIN)
    @Max(CarModelYear.MAX)
    private Integer yearTo;
    private Double priceFrom;
    private Double priceTo;
    private String color;
    private String featureCategory;

    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private Sort.Direction sortDirection = Sort.Direction.ASC;
}