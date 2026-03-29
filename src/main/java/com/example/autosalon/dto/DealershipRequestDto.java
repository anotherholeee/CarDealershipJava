package com.example.autosalon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "DTO для создания или обновления автосалона")
public class DealershipRequestDto {

    @Schema(description = "Название автосалона", example = "Auto Premium")
    @NotBlank(message = "Название автосалона не может быть пустым")
    @Size(min = 3, max = 200, message = "Название должно содержать от 3 до 200 символов")
    private String name;

    @Schema(description = "Адрес автосалона", example = "Moscow, Tverskaya 12")
    @NotBlank(message = "Адрес не может быть пустым")
    @Size(max = 300, message = "Адрес не может превышать 300 символов")
    private String address;

    @Schema(description = "Телефон автосалона", example = "+74951234567")
    @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,20}$", message = "Некорректный формат телефона")
    private String phone;
}