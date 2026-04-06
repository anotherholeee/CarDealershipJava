package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealershipServiceTest {

    @Mock private DealershipRepository dealershipRepository;
    @Mock private CarRepository carRepository;
    @Mock private CarService carService;
    @Mock private DealershipTransactionalService dealershipTransactionalService;
    @InjectMocks private DealershipService dealershipService;

    private Dealership dealership;
    private Car car;

    @BeforeEach
    void setUp() {
        dealership = new Dealership();
        dealership.setId(1L);
        dealership.setName("AutoCenter");
        dealership.setAddress("Moscow");
        dealership.setPhone("123");

        car = new Car();
        car.setId(10L);
        car.setBrand("Toyota");
    }

    @Test
    void getAllDealerships_shouldReturnList() {
        when(dealershipRepository.findAll()).thenReturn(List.of(dealership));
        List<Dealership> result = dealershipService.getAllDealerships();
        assertThat(result).hasSize(1);
    }

    @Test
    void getDealershipById_notFound_throws() {
        when(dealershipRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> dealershipService.getDealershipById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createDealership_shouldSave() {
        when(dealershipRepository.save(any(Dealership.class))).thenReturn(dealership);
        Dealership created = dealershipService.createDealership(dealership);
        assertThat(created).isEqualTo(dealership);
    }

    @Test
    void getDealershipWithCars_shouldReturn() {
        when(dealershipRepository.findByIdWithCars(1L)).thenReturn(Optional.of(dealership));
        Dealership result = dealershipService.getDealershipWithCars(1L);
        assertThat(result).isEqualTo(dealership);
    }

    @Test
    void updateDealership_shouldUpdateFields() {
        Dealership details = new Dealership();
        details.setName("NewName");
        details.setAddress("Addr");
        details.setPhone("999");

        when(dealershipTransactionalService.getDealershipById(1L)).thenReturn(dealership);
        Dealership updated = dealershipService.updateDealership(1L, details);
        assertThat(updated.getName()).isEqualTo("NewName");
        assertThat(updated.getAddress()).isEqualTo("Addr");
        assertThat(updated.getPhone()).isEqualTo("999");
    }

    @Test
    void addCarToDealership_shouldAdd() {
        when(dealershipRepository.findByIdWithCars(1L)).thenReturn(Optional.of(dealership));
        when(carRepository.findById(10L)).thenReturn(Optional.of(car));
        Dealership updated = dealershipService.addCarToDealership(1L, 10L);
        assertThat(updated.getCars()).contains(car);
        assertThat(car.getDealership()).isEqualTo(dealership);
    }

    @Test
    void addCarToDealership_carNotFound_throws() {
        when(dealershipRepository.findByIdWithCars(1L)).thenReturn(Optional.of(dealership));
        when(carRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> dealershipService.addCarToDealership(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeCarFromDealership_shouldRemove() {
        dealership.addCar(car);
        when(dealershipRepository.findByIdWithCars(1L)).thenReturn(Optional.of(dealership));
        when(carRepository.findById(10L)).thenReturn(Optional.of(car));
        Dealership updated = dealershipService.removeCarFromDealership(1L, 10L);
        assertThat(updated.getCars()).doesNotContain(car);
        assertThat(car.getDealership()).isNull();
    }

    @Test
    void countDealerships_shouldReturnCount() {
        when(dealershipRepository.count()).thenReturn(5L);
        assertThat(dealershipService.countDealerships()).isEqualTo(5L);
    }

    @Test
    void deleteDealership_shouldDeleteCarsFirst() {
        dealership.setCars(List.of(car));
        when(dealershipRepository.findByIdWithCars(1L)).thenReturn(Optional.of(dealership));
        doNothing().when(carService).deleteCar(10L);
        dealershipService.deleteDealership(1L);
        verify(carService).deleteCar(10L);
        verify(dealershipRepository).delete(dealership);
    }
}