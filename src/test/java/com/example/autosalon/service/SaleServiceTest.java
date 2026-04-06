package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private CarService carService;
    @Mock private CustomerService customerService;
    @InjectMocks private SaleService saleService;

    private Sale sale;
    private Car car;
    private Customer customer;

    @BeforeEach
    void setUp() {
        car = new Car();
        car.setId(1L);
        car.setBrand("BMW");

        customer = new Customer();
        customer.setId(2L);
        customer.setFirstName("Alice");

        sale = new Sale();
        sale.setId(10L);
        sale.setCar(car);
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDateTime.now());
        sale.setSalePrice(50000.0);
    }

    @Test
    void getAllSales_shouldReturnList() {
        when(saleRepository.findAll()).thenReturn(List.of(sale));
        List<Sale> sales = saleService.getAllSales();
        assertThat(sales).hasSize(1);
    }

    @Test
    void getSaleById_notFound_throws() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> saleService.getSaleById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createSale_carAlreadySold_throws() {
        car.setSale(new Sale()); // уже продана
        when(carService.getCarById(1L)).thenReturn(car);
        assertThatThrownBy(() -> saleService.createSale(sale))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже продана");
    }

    @Test
    void updateSale_changeCustomer_shouldUpdate() {
        Customer newCustomer = new Customer();
        newCustomer.setId(3L);
        Sale updatedDetails = new Sale();
        updatedDetails.setCustomer(newCustomer);
        updatedDetails.setSaleDate(LocalDateTime.now());
        updatedDetails.setSalePrice(55000.0);

        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));
        when(customerService.getCustomerById(3L)).thenReturn(newCustomer);
        Sale result = saleService.updateSale(10L, updatedDetails);
        assertThat(result.getCustomer()).isEqualTo(newCustomer);
    }

    @Test
    void deleteSale_shouldRemoveReferences() {
        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));
        saleService.deleteSale(10L);
        assertThat(car.getSale()).isNull();
        assertThat(customer.getSales()).doesNotContain(sale);
        verify(saleRepository).delete(sale);
    }
}