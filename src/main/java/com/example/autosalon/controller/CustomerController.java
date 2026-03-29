package com.example.autosalon.controller;

import com.example.autosalon.dto.CustomerRequestDto;
import com.example.autosalon.dto.CustomerResponseDto;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.mapper.CustomerMapper;
import com.example.autosalon.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Операции с клиентами")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    @GetMapping
    @Operation(summary = "Получить список клиентов")
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponseDto> response = customers.stream()
                .map(customerMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить клиента по ID")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customerMapper.toResponseDto(customer));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Получить клиента по email")
    public ResponseEntity<CustomerResponseDto> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(customerMapper.toResponseDto(customer));
    }

    @PostMapping
    @Operation(summary = "Создать клиента")
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CustomerRequestDto requestDto) {
        Customer customer = customerMapper.toEntity(requestDto);
        Customer createdCustomer = customerService.createCustomer(customer);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(createdCustomer);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить клиента")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDto requestDto) {
        Customer customerDetails = customerMapper.toEntity(requestDto);
        Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(updatedCustomer);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/{id}/phone")
    @Operation(summary = "Обновить телефон клиента")
    public ResponseEntity<CustomerResponseDto> updateCustomerPhone(
            @PathVariable Long id,
            @RequestParam String phone) {
        Customer updatedCustomer = customerService.updatePhone(id, phone);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(updatedCustomer);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить клиента")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}