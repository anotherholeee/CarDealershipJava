package com.example.autosalon.dto;

import com.example.autosalon.enums.AccountType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminUserResponseDto {
    private Long id;
    private String username;
    private AccountType accountType;
    private String displayName;
    private String personName;
    private String companyName;
    private String phone;
    private String address;
}
