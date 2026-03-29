package com.example.autosalon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "DTO для создания или обновления клиента")
public class CustomerRequestDto {

    @Schema(description = "Имя клиента", example = "Ivan")
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов")
    private String firstName;

    @Schema(description = "Фамилия клиента", example = "Petrov")
    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(min = 2, max = 100, message = "Фамилия должна содержать от 2 до 100 символов")
    private String lastName;

    @Schema(description = "Email клиента", example = "ivan.petrov@example.com")
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    private String email;

    @Schema(description = "Телефон клиента", example = "+79991234567")
    @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,20}$", message = "Некорректный формат телефона")
    private String phone;
}