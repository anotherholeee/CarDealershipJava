package com.example.autosalon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarImageInfoDto {
    private Long id;
    /** Относительный URL (путь), например /api/cars/1/photos/uuid.jpg */
    private String url;
}
