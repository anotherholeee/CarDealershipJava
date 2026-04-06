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
import java.util.ArrayList;
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
    void getSaleById_shouldReturnSale() {
        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));

        Sale result = saleService.getSaleById(10L);

        assertThat(result).isEqualTo(sale);
    }

    @Test
    void getSaleByCarId_shouldReturnSale() {
        when(saleRepository.findByCarId(1L)).thenReturn(Optional.of(sale));

        Sale result = saleService.getSaleByCarId(1L);

        assertThat(result).isEqualTo(sale);
    }

    @Test
    void getSaleByCarId_notFound_throws() {
        when(saleRepository.findByCarId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.getSaleByCarId(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSalesByCustomerId_shouldReturnList() {
        when(saleRepository.findByCustomerId(2L)).thenReturn(List.of(sale));

        List<Sale> result = saleService.getSalesByCustomerId(2L);

        assertThat(result).containsExactly(sale);
    }

    @Test
    void getSalesByDateRange_shouldReturnList() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        when(saleRepository.findBySaleDateBetween(start, end)).thenReturn(List.of(sale));

        List<Sale> result = saleService.getSalesByDateRange(start, end);

        assertThat(result).containsExactly(sale);
    }

    @Test
    void createSale_shouldSaveAndBindRelations() {
        customer.setSales(new ArrayList<>());
        when(carService.getCarById(1L)).thenReturn(car);
        when(customerService.getCustomerById(2L)).thenReturn(customer);
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        Sale created = saleService.createSale(sale);

        assertThat(created).isEqualTo(sale);
        assertThat(car.getSale()).isEqualTo(sale);
        assertThat(customer.getSales()).contains(sale);
        verify(saleRepository).save(sale);
    }

    @Test
    void createSale_withoutCar_throws() {
        sale.setCar(null);

        assertThatThrownBy(() -> saleService.createSale(sale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Car must be specified");
    }

    @Test
    void createSale_withoutCustomer_throws() {
        sale.setCustomer(null);

        assertThatThrownBy(() -> saleService.createSale(sale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer must be specified");
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
        newCustomer.setSales(new ArrayList<>());
        customer.setSales(new ArrayList<>(List.of(sale)));
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
    void updateSale_notFound_throws() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.updateSale(99L, new Sale()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateSale_changeCar_throws() {
        Car anotherCar = new Car();
        anotherCar.setId(99L);
        Sale updatedDetails = new Sale();
        updatedDetails.setCar(anotherCar);
        updatedDetails.setSaleDate(LocalDateTime.now());
        updatedDetails.setSalePrice(52000.0);

        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> saleService.updateSale(10L, updatedDetails))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нельзя изменить машину");
    }

    @Test
    void updateSale_withoutCustomerChange_updatesMainFields() {
        Sale updatedDetails = new Sale();
        updatedDetails.setSaleDate(LocalDateTime.now().plusDays(1));
        updatedDetails.setSalePrice(51000.0);

        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));

        Sale result = saleService.updateSale(10L, updatedDetails);

        assertThat(result.getSaleDate()).isEqualTo(updatedDetails.getSaleDate());
        assertThat(result.getSalePrice()).isEqualTo(51000.0);
        assertThat(result.getCustomer()).isEqualTo(customer);
    }

    @Test
    void deleteSale_shouldRemoveReferences() {
        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));
        saleService.deleteSale(10L);
        assertThat(car.getSale()).isNull();
        assertThat(customer.getSales()).doesNotContain(sale);
        verify(saleRepository).delete(sale);
    }

    @Test
    void deleteSale_notFound_throws() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.deleteSale(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteSale_whenCarAndCustomerAreNull_deletesSafely() {
        Sale saleWithoutLinks = new Sale();
        saleWithoutLinks.setId(11L);
        saleWithoutLinks.setCar(null);
        saleWithoutLinks.setCustomer(null);

        when(saleRepository.findById(11L)).thenReturn(Optional.of(saleWithoutLinks));

        saleService.deleteSale(11L);

        verify(saleRepository).delete(saleWithoutLinks);
    }
}