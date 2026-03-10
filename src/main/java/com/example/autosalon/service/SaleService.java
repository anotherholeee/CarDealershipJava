package com.example.autosalon.service;

import com.example.autosalon.entity.Sale;
import com.example.autosalon.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final CarService carService;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Sale getSaleByCarId(Long carId) {
        return saleRepository.findByCarId(carId)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found for car id: " + carId));
    }

    @Transactional(readOnly = true)
    public List<Sale> getSalesByCustomerId(Long customerId) {
        return saleRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Sale> getSalesByDateRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findBySaleDateBetween(start, end);
    }

    @Transactional
    public Sale createSale(Sale sale) {
        sale.setId(null);


        if (sale.getCar() != null && sale.getCar().getId() != null) {
            carService.getCarById(sale.getCar().getId());
        }

        if (sale.getCustomer() != null && sale.getCustomer().getId() != null) {
            customerService.getCustomerById(sale.getCustomer().getId());
        }

        return saleRepository.save(sale);
    }

    @Transactional
    public Sale updateSale(Long id, Sale saleDetails) {
        Sale existingSale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found with id: " + id));

        existingSale.setSaleDate(saleDetails.getSaleDate());
        existingSale.setSalePrice(saleDetails.getSalePrice());

        if (saleDetails.getCar() != null) {
            existingSale.setCar(saleDetails.getCar());
        }
        if (saleDetails.getCustomer() != null) {
            existingSale.setCustomer(saleDetails.getCustomer());
        }

        return existingSale;
    }

    @Transactional
    public void deleteSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found with id: " + id));

        if (sale.getCar() != null) {
            sale.getCar().setSale(null);
        }

        if (sale.getCustomer() != null) {
            sale.getCustomer().getSales().remove(sale);
        }

        saleRepository.delete(sale);
        log.info("Продажа с id {} успешно удалена", id);
    }
}