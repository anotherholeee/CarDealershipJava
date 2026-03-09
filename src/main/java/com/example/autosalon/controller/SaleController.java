package com.example.autosalon.controller;

import com.example.autosalon.dto.SaleRequestDto;
import com.example.autosalon.dto.SaleResponseDto;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.mapper.SaleMapper;
import com.example.autosalon.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final SaleMapper saleMapper;

    @GetMapping
    public ResponseEntity<List<SaleResponseDto>> getAllSales() {
        List<Sale> sales = saleService.getAllSales();
        List<SaleResponseDto> responseDtos = sales.stream()
                .map(saleMapper::toResponseDto)
                .sorted(Comparator.comparing(SaleResponseDto::getId))
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponseDto> getSaleById(@PathVariable Long id) {
        Sale sale = saleService.getSaleById(id);
        return ResponseEntity.ok(saleMapper.toResponseDto(sale));
    }

    @GetMapping("/car/{carId}")
    public ResponseEntity<SaleResponseDto> getSaleByCarId(@PathVariable Long carId) {
        Sale sale = saleService.getSaleByCarId(carId);
        return ResponseEntity.ok(saleMapper.toResponseDto(sale));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SaleResponseDto>> getSalesByCustomerId(@PathVariable Long customerId) {
        List<Sale> sales = saleService.getSalesByCustomerId(customerId);
        List<SaleResponseDto> responseDtos = sales.stream()
                .map(saleMapper::toResponseDto)
                .sorted(Comparator.comparing(SaleResponseDto::getId))
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<SaleResponseDto>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Sale> sales = saleService.getSalesByDateRange(start, end);
        List<SaleResponseDto> responseDtos = sales.stream()
                .map(saleMapper::toResponseDto)
                .sorted(Comparator.comparing(SaleResponseDto::getId))
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    @PostMapping
    public ResponseEntity<SaleResponseDto> createSale(@RequestBody SaleRequestDto createDto) {
        Sale sale = saleMapper.toEntity(createDto);
        Sale savedSale = saleService.createSale(sale);
        SaleResponseDto responseDto = saleMapper.toResponseDto(savedSale);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleResponseDto> updateSale(@PathVariable Long id, @RequestBody SaleRequestDto updateDto) {
        Sale saleDetails = saleMapper.toEntity(updateDto);
        Sale updatedSale = saleService.updateSale(id, saleDetails);
        return ResponseEntity.ok(saleMapper.toResponseDto(updatedSale));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }
}