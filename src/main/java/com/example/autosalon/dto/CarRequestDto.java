package com.example.autosalon.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для создания или обновления автомобиля")
public class CarRequestDto {

    @Schema(description = "Бренд автомобиля", example = "Toyota")
    @NotBlank(message = "Бренд не может быть пустым")
    @Size(min = 2, max = 100, message = "Бренд должен содержать от 2 до 100 символов")
    private String brand;

    @Schema(description = "Модель автомобиля", example = "Camry")
    @NotBlank(message = "Модель не может быть пустой")
    @Size(min = 1, max = 100, message = "Модель должна содержать от 1 до 100 символов")
    private String model;

    @Schema(description = "Год выпуска", example = "2022")
    @NotNull(message = "Год выпуска обязателен")
    @Min(value = 1886, message = "Год выпуска не может быть меньше 1886")
    @Max(value = 2026, message = "Год выпуска не может быть больше 2026")
    private Integer year;

    @Schema(description = "Цвет автомобиля", example = "Black")
    @NotBlank(message = "Цвет не может быть пустым")
    private String color;

    @Schema(description = "Цена автомобиля", example = "3200000")
    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительной")
    private Double price;

    @ArraySchema(schema = @Schema(description = "Список ID опций", example = "1"))
    private List<Long> featureIds;
}