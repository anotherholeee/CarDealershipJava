package com.example.autosalon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminPasswordChangeDto {
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 4, max = 120, message = "Пароль от 4 до 120 символов")
    private String newPassword;
}
