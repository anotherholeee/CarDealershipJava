package com.example.autosalon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Единый формат ошибки API")
public class ErrorResponseDto {
    @Schema(description = "Время возникновения ошибки", example = "2026-03-29 20:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    @Schema(description = "HTTP статус-код", example = "400")
    private int status;
    @Schema(description = "Краткое название ошибки", example = "Bad Request")
    private String error;
    @Schema(description = "Подробное сообщение об ошибке", example = "Некорректный формат email")
    private String message;
    @Schema(description = "Путь запроса", example = "/api/customers")
    private String path;
    @Schema(description = "HTTP метод запроса", example = "POST")
    private String method;
}