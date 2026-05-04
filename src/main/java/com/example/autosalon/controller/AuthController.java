package com.example.autosalon.controller;

import com.example.autosalon.dto.AuthLoginRequestDto;
import com.example.autosalon.dto.AuthRegisterRequestDto;
import com.example.autosalon.dto.AuthResponseDto;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Регистрация и авторизация")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody AuthRegisterRequestDto request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход пользователя")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthLoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Получить текущего пользователя по токену")
    public ResponseEntity<AuthResponseDto> me(@RequestHeader("Authorization") String authorization) {
        UserAccount user = authService.requireUserByToken(authorization);
        return ResponseEntity.ok(authService.toAuthResponse(user, null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из сессии")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
