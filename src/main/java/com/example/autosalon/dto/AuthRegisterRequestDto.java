package com.example.autosalon.dto;

import com.example.autosalon.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegisterRequestDto {
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 4, max = 120, message = "Пароль должен быть длиной от 4 до 120 символов")
    private String password;

    @NotNull(message = "Тип аккаунта обязателен")
    private AccountType accountType;

    @Size(max = 120, message = "Имя должно быть не длиннее 120 символов")
    private String personName;

    @Size(max = 200, message = "Название компании должно быть не длиннее 200 символов")
    private String companyName;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(
            regexp = "^\\+375\\d{9}$",
            message = "Телефон должен быть в формате +375XXXXXXXXX (ровно 13 символов)"
    )
    private String phone;

    @Size(max = 255, message = "Адрес должен быть не длиннее 255 символов")
    private String address;
}
