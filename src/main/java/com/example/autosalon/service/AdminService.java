package com.example.autosalon.service;

import com.example.autosalon.dto.AdminPasswordChangeDto;
import com.example.autosalon.dto.AdminUserResponseDto;
import com.example.autosalon.dto.AdminUserUpdateDto;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.enums.AccountType;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.UserAccountRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final CarRepository carRepository;
    private final CarService carService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional(readOnly = true)
    public List<AdminUserResponseDto> listUsers() {
        return userAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getId))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponseDto getUser(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: id=" + id));
        return toDto(user);
    }

    @Transactional
    public AdminUserResponseDto updateUser(Long id, AdminUserUpdateDto dto) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: id=" + id));
        if (dto.getAccountType() == AccountType.ADMIN) {
            throw new IllegalArgumentException("Нельзя назначить тип ADMIN через API");
        }
        if (dto.getPersonName() != null) {
            user.setPersonName(dto.getPersonName().trim().isEmpty() ? null : dto.getPersonName().trim());
        }
        if (dto.getCompanyName() != null) {
            user.setCompanyName(dto.getCompanyName().trim().isEmpty() ? null : dto.getCompanyName().trim());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress().trim().isEmpty() ? null : dto.getAddress().trim());
        }
        if (dto.getAccountType() != null) {
            if (user.getAccountType() == AccountType.ADMIN) {
                throw new IllegalArgumentException("Нельзя менять тип учётной записи администратора");
            }
            user.setAccountType(dto.getAccountType());
        }
        return toDto(user);
    }

    @Transactional
    public void setPassword(Long id, AdminPasswordChangeDto dto, UserAccount adminActor) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: id=" + id));
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        if (user.getId().equals(adminActor.getId())) {
            // ok — смена своего пароля
        }
    }

    @Transactional
    public void deleteUser(Long id, UserAccount adminActor) {
        if (id.equals(adminActor.getId())) {
            throw new IllegalStateException("Нельзя удалить собственную учётную запись");
        }
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: id=" + id));
        if (user.getAccountType() == AccountType.ADMIN) {
            long admins = userAccountRepository.countByAccountType(AccountType.ADMIN);
            if (admins <= 1) {
                throw new IllegalStateException("Нельзя удалить последнего администратора");
            }
        }
        carRepository.findByOwnerId(user.getId()).forEach(car ->
                carService.deleteCar(car.getId(), adminActor));
        userAccountRepository.delete(user);
    }

    private AdminUserResponseDto toDto(UserAccount user) {
        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAccountType(user.getAccountType());
        dto.setPersonName(user.getPersonName());
        dto.setCompanyName(user.getCompanyName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setDisplayName(resolveDisplay(user));
        return dto;
    }

    private static String resolveDisplay(UserAccount user) {
        if (user.getAccountType() == AccountType.ADMIN) {
            return user.getPersonName() != null && !user.getPersonName().isBlank()
                    ? user.getPersonName().trim()
                    : "Администратор";
        }
        if (user.getAccountType() == AccountType.DEALERSHIP) {
            return user.getCompanyName();
        }
        return user.getPersonName();
    }
}
