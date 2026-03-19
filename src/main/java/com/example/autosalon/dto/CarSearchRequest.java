package com.example.autosalon.dto;

import lombok.Data;
import org.springframework.data.domain.Sort;

/**
 * DTO для запроса поиска автомобилей с фильтрацией и пагинацией
 */
@Data
public class CarSearchRequest {

    private String brand;
    private String model;
    private Integer yearFrom;
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