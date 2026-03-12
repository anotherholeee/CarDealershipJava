package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.repository.SaleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private static final String SALE_NOT_FOUND_MESSAGE = "Sale not found with id: ";

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
                .orElseThrow(() -> new IllegalArgumentException(SALE_NOT_FOUND_MESSAGE + id));
    }

    @Transactional(readOnly = true)
    public Sale getSaleByCarId(Long carId) {
        return saleRepository.findByCarId(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sale not found for car id: " + carId));
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

        if (sale.getCar() == null || sale.getCar().getId() == null) {
            throw new IllegalArgumentException("Car must be specified for sale");
        }
        if (sale.getCustomer() == null || sale.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer must be specified for sale");
        }

        Car car = carService.getCarById(sale.getCar().getId());
        if (car.getSale() != null) {
            throw new IllegalStateException(
                    String.format("Машина ID=%d %s %s %d уже продана (ID продажи: %d)",
                            car.getId(),
                            car.getBrand(),
                            car.getModel(),
                            car.getYear(),
                            car.getSale().getId()));
        }

        Customer customer = customerService.getCustomerById(sale.getCustomer().getId());

        sale.setCar(car);
        car.setSale(sale);

        sale.setCustomer(customer);
        customer.getSales().add(sale);

        return saleRepository.save(sale);
    }

    @Transactional
    public Sale updateSale(Long id, Sale saleDetails) {
        Sale existingSale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(SALE_NOT_FOUND_MESSAGE + id));

        existingSale.setSaleDate(saleDetails.getSaleDate());
        existingSale.setSalePrice(saleDetails.getSalePrice());

        if (saleDetails.getCar() != null
                && saleDetails.getCar().getId() != null
                && !saleDetails.getCar().getId().equals(
                        existingSale.getCar() != null ? existingSale.getCar().getId() : null)) {
            throw new IllegalStateException("Нельзя изменить машину у существующей продажи");
        }

        if (saleDetails.getCustomer() != null && saleDetails.getCustomer().getId() != null) {
            Customer newCustomer = customerService.getCustomerById(
                    saleDetails.getCustomer().getId());

            Customer oldCustomer = existingSale.getCustomer();
            if (oldCustomer != null && !oldCustomer.equals(newCustomer)) {
                oldCustomer.getSales().remove(existingSale);
            }

            existingSale.setCustomer(newCustomer);
            if (!newCustomer.getSales().contains(existingSale)) {
                newCustomer.getSales().add(existingSale);
            }
        }

        return existingSale;
    }

    @Transactional
    public void deleteSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(SALE_NOT_FOUND_MESSAGE + id));

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