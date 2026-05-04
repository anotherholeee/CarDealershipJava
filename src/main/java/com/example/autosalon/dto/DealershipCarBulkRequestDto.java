package com.example.autosalon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class DealershipCarBulkRequestDto {

    @NotEmpty(message = "Список автомобилей не может быть пустым")
    @Size(min = 1, max = 30, message = "За один раз можно добавить от 1 до 30 объявлений")
    private List<@Valid CarRequestDto> cars;
}
