package com.example.autosalon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "DTO для создания или обновления опции автомобиля")
public class FeatureRequestDto {

    @Schema(description = "Название опции", example = "Подогрев сидений")
    @NotBlank(message = "Название особенности не может быть пустым")
    @Size(min = 3, max = 100, message = "Название должно содержать от 3 до 100 символов")
    private String name;

    @Schema(description = "Описание опции", example = "Подогрев передних и задних сидений")
    @Size(max = 500, message = "Описание не может превышать 500 символов")
    private String description;

    @Schema(description = "Категория опции", example = "Комфорт")
    @NotBlank(message = "Категория не может быть пустой")
    private String category;
}