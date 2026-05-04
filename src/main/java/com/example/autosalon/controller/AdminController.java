package com.example.autosalon.controller;

import com.example.autosalon.dto.AdminPasswordChangeDto;
import com.example.autosalon.dto.AdminUserResponseDto;
import com.example.autosalon.dto.AdminUserUpdateDto;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.service.AdminService;
import com.example.autosalon.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Управление пользователями (только ADMIN)")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;

    @GetMapping("/users")
    @Operation(summary = "Список всех аккаунтов")
    public ResponseEntity<List<AdminUserResponseDto>> listUsers(
            @RequestHeader("Authorization") String authorization) {
        authService.requireAdminByToken(authorization);
        return ResponseEntity.ok(adminService.listUsers());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Аккаунт по id")
    public ResponseEntity<AdminUserResponseDto> getUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        authService.requireAdminByToken(authorization);
        return ResponseEntity.ok(adminService.getUser(id));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Обновить профиль пользователя")
    public ResponseEntity<AdminUserResponseDto> updateUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateDto dto) {
        authService.requireAdminByToken(authorization);
        return ResponseEntity.ok(adminService.updateUser(id, dto));
    }

    @PutMapping("/users/{id}/password")
    @Operation(summary = "Сбросить пароль пользователя")
    public ResponseEntity<Void> setPassword(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordChangeDto dto) {
        UserAccount admin = authService.requireAdminByToken(authorization);
        adminService.setPassword(id, dto, admin);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Удалить пользователя и все его объявления")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        UserAccount admin = authService.requireAdminByToken(authorization);
        adminService.deleteUser(id, admin);
        return ResponseEntity.noContent().build();
    }
}
