package com.example.autosalon.mapper;

import com.example.autosalon.dto.SaleRequestDto;
import com.example.autosalon.dto.SaleResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.service.CarService;
import com.example.autosalon.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleMapper {

    private final CarService carService;
    private final CustomerService customerService;

    public SaleResponseDto toResponseDto(Sale sale) {
        if (sale == null) {
            return null;
        }

        SaleResponseDto dto = new SaleResponseDto();
        dto.setId(sale.getId());
        dto.setSaleDate(sale.getSaleDate());
        dto.setSalePrice(sale.getSalePrice());

        if (sale.getCar() != null) {
            dto.setCarId(sale.getCar().getId());
            dto.setCarBrand(sale.getCar().getBrand());
            dto.setCarModel(sale.getCar().getModel());
        }

        if (sale.getCustomer() != null) {
            dto.setCustomerId(sale.getCustomer().getId());
            dto.setCustomerName(sale.getCustomer().getFirstName() + " " + sale.getCustomer().getLastName());
        }

        return dto;
    }

    public Sale toEntity(SaleRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Sale sale = new Sale();
        sale.setSaleDate(dto.getSaleDate());
        sale.setSalePrice(dto.getSalePrice());

        if (dto.getCarId() != null) {
            Car car = carService.getCarById(dto.getCarId());
            sale.setCar(car);
        }

        if (dto.getCustomerId() != null) {
            Customer customer = customerService.getCustomerById(dto.getCustomerId());
            sale.setCustomer(customer);
        }

        return sale;
    }
}