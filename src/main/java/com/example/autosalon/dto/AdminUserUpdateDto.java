package com.example.autosalon.dto;

import com.example.autosalon.enums.AccountType;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminUserUpdateDto {

    @Size(max = 120)
    private String personName;

    @Size(max = 200)
    private String companyName;

    @Size(max = 255)
    private String address;

    /** Смена типа: только PERSON или DEALERSHIP (не ADMIN). */
    private AccountType accountType;
}
