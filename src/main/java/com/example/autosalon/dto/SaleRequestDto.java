package com.example.autosalon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для создания или обновления продажи")
public class SaleRequestDto {

    @Schema(description = "ID автомобиля", example = "10")
    @NotNull(message = "ID автомобиля обязателен")
    private Long carId;

    @Schema(description = "ID покупателя", example = "7")
    @NotNull(message = "ID покупателя обязателен")
    private Long customerId;

    @Schema(description = "Дата продажи в формате ISO-8601", example = "2026-03-01T12:30:00")
    @NotNull(message = "Дата продажи обязательна")
    @PastOrPresent(message = "Дата продажи не может быть в будущем")
    private LocalDateTime saleDate;

    @Schema(description = "Итоговая цена продажи", example = "2999000")
    @NotNull(message = "Цена продажи обязательна")
    @Positive(message = "Цена продажи должна быть положительной")
    private Double salePrice;
}