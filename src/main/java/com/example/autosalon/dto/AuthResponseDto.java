package com.example.autosalon.dto;

import com.example.autosalon.enums.AccountType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponseDto {
    private String token;
    private String username;
    private AccountType accountType;
    private String displayName;
    /** Для физического лица */
    private String personName;
    /** Для автосалона */
    private String companyName;
    private String address;
    /** Телефон (совпадает с username) */
    private String phone;
}
