package com.example.autosalon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CarListRequestDto {
    @NotEmpty(message = "Список автомобилей не может быть пустым")
    private List<@Valid CarRequestDto> cars;
}