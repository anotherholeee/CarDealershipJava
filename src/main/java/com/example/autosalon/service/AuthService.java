package com.example.autosalon.service;

import com.example.autosalon.dto.AuthLoginRequestDto;
import com.example.autosalon.dto.AuthRegisterRequestDto;
import com.example.autosalon.dto.AuthResponseDto;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.enums.AccountType;
import com.example.autosalon.repository.DealershipRepository;
import com.example.autosalon.repository.UserAccountRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final DealershipRepository dealershipRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    @Transactional
    public AuthResponseDto register(AuthRegisterRequestDto request) {
        if (request.getAccountType() == AccountType.ADMIN) {
            throw new IllegalArgumentException("Нельзя зарегистрироваться с типом администратора");
        }
        String login = normalizePhone(request.getPhone());
        validateByAccountType(request);

        if (userAccountRepository.existsByUsernameIgnoreCase(login)) {
            throw new IllegalStateException("Пользователь с таким логином уже существует");
        }

        UserAccount user = new UserAccount();
        user.setUsername(login);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountType(request.getAccountType());
        user.setPersonName(trimToNull(request.getPersonName()));
        user.setCompanyName(trimToNull(request.getCompanyName()));
        user.setPhone(login);
        user.setAddress(trimToNull(request.getAddress()));
        UserAccount saved = userAccountRepository.save(user);
        createDealershipForCompanyAccount(saved);

        String token = issueToken(saved.getId());
        return toAuthResponse(saved, token);
    }

    public AuthResponseDto login(AuthLoginRequestDto request) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный логин или пароль");
        }

        String token = issueToken(user.getId());
        return toAuthResponse(user, token);
    }

    /** Собирает DTO профиля; для {@code /me} передайте {@code token = null}. */
    public AuthResponseDto toAuthResponse(UserAccount user, String token) {
        AuthResponseDto dto = new AuthResponseDto();
        dto.setToken(token);
        dto.setUsername(user.getUsername());
        dto.setAccountType(user.getAccountType());
        dto.setDisplayName(resolveDisplayName(user));
        dto.setPersonName(user.getPersonName());
        dto.setCompanyName(user.getCompanyName());
        dto.setAddress(user.getAddress());
        dto.setPhone(user.getPhone());
        return dto;
    }

    public UserAccount requireUserByToken(String tokenHeader) {
        String token = extractBearerToken(tokenHeader);
        Long userId = tokenToUserId.get(token);
        if (userId == null) {
            throw new IllegalArgumentException("Сессия недействительна. Войдите снова.");
        }
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    public UserAccount requireAdminByToken(String tokenHeader) {
        UserAccount user = requireUserByToken(tokenHeader);
        if (user.getAccountType() != AccountType.ADMIN) {
            throw new IllegalArgumentException("Требуются права администратора");
        }
        return user;
    }

    public static boolean isAdmin(UserAccount user) {
        return user != null && user.getAccountType() == AccountType.ADMIN;
    }

    public void logout(String tokenHeader) {
        String token = extractBearerToken(tokenHeader);
        tokenToUserId.remove(token);
    }

    private String issueToken(Long userId) {
        String token = UUID.randomUUID().toString();
        tokenToUserId.put(token, userId);
        return token;
    }

    private String extractBearerToken(String tokenHeader) {
        if (tokenHeader == null || tokenHeader.isBlank()) {
            throw new IllegalArgumentException("Требуется Authorization: Bearer <token>");
        }
        String prefix = "Bearer ";
        if (!tokenHeader.startsWith(prefix) || tokenHeader.length() <= prefix.length()) {
            throw new IllegalArgumentException("Некорректный формат Authorization заголовка");
        }
        return tokenHeader.substring(prefix.length()).trim();
    }

    private void validateByAccountType(AuthRegisterRequestDto request) {
        String phone = trimToNull(request.getPhone());
        if (phone == null) {
            throw new IllegalArgumentException("Телефон обязателен");
        }
        if (!isValidBelarusPhone(phone)) {
            throw new IllegalArgumentException("Телефон должен быть строго в формате +375XXXXXXXXX");
        }
        if (request.getAccountType() == AccountType.PERSON) {
            if (trimToNull(request.getPersonName()) == null) {
                throw new IllegalArgumentException("Имя на кириллице обязательно для физ.лица");
            }
            return;
        }
        if (trimToNull(request.getCompanyName()) == null) {
            throw new IllegalArgumentException("Название компании обязательно для юр.лица");
        }
        if (trimToNull(request.getAddress()) == null) {
            throw new IllegalArgumentException("Адрес обязателен для юр.лица");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePhone(String phone) {
        String cleaned = trimToNull(phone);
        if (cleaned == null) {
            return "";
        }
        return cleaned;
    }

    private boolean isValidBelarusPhone(String phone) {
        return phone.matches("^\\+375\\d{9}$");
    }

    private String resolveDisplayName(UserAccount user) {
        if (user.getAccountType() == AccountType.ADMIN) {
            String n = trimToNull(user.getPersonName());
            return n != null ? n : "Администратор";
        }
        if (user.getAccountType() == AccountType.DEALERSHIP) {
            return trimToNull(user.getCompanyName());
        }
        return trimToNull(user.getPersonName());
    }

    private void createDealershipForCompanyAccount(UserAccount user) {
        if (user.getAccountType() != AccountType.DEALERSHIP) {
            return;
        }
        Dealership dealership = new Dealership();
        dealership.setName(trimToNull(user.getCompanyName()));
        dealership.setAddress(trimToNull(user.getAddress()));
        dealership.setPhone(trimToNull(user.getPhone()));
        dealershipRepository.save(dealership);
    }
}
