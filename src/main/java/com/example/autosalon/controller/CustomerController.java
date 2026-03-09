package com.example.autosalon.controller;
import com.example.autosalon.dto.CustomerRequestDto;
import com.example.autosalon.dto.CustomerResponseDto;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.mapper.CustomerMapper;
import com.example.autosalon.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponseDto> response = customers.stream()
                .map(customerMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customerMapper.toResponseDto(customer));
    }
    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerResponseDto> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(customerMapper.toResponseDto(customer));
    }
    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(@RequestBody CustomerRequestDto requestDto) {
        Customer customer = customerMapper.toEntity(requestDto);
        Customer createdCustomer = customerService.createCustomer(customer);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(createdCustomer);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long id,
            @RequestBody CustomerRequestDto requestDto
    ) {
        Customer customerDetails = customerMapper.toEntity(requestDto);
        Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(updatedCustomer);
        return ResponseEntity.ok(responseDto);
    }
    @PatchMapping("/{id}/phone")
    public ResponseEntity<CustomerResponseDto> updateCustomerPhone(
            @PathVariable Long id,
            @RequestParam String phone
    ) {
        Customer updatedCustomer = customerService.updatePhone(id, phone);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(updatedCustomer);
        return ResponseEntity.ok(responseDto);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}