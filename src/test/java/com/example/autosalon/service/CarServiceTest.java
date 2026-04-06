package com.example.autosalon.service;

import com.example.autosalon.cache.CarSearchCache;
import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock private CarRepository carRepository;
    @Mock private CarMapper carMapper;
    @Mock private CarSearchCache searchCache;
    @Mock private ObjectProvider<CarService> selfProvider;
    @InjectMocks private CarService carService;

    private Car car;
    private CarRequestDto requestDto;
    private CarResponseDto responseDto;

    @BeforeEach
    void setUp() {
        car = new Car();
        car.setId(1L);
        car.setBrand("BMW");
        car.setModel("X5");
        car.setYear(2023);
        car.setPrice(60000.0);

        requestDto = new CarRequestDto();
        requestDto.setBrand("BMW");
        requestDto.setModel("X5");
        requestDto.setYear(2023);
        requestDto.setPrice(60000.0);

        responseDto = new CarResponseDto();
        responseDto.setId(1L);
        responseDto.setBrand("BMW");
        responseDto.setModel("X5");
    }

    @Test
    void getCarById_shouldReturnCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        Car found = carService.getCarById(1L);
        assertThat(found).isEqualTo(car);
    }

    @Test
    void getCarById_notFound_throwsException() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> carService.getCarById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Машина с id 99 не найдена");
    }

    @Test
    void createCar_shouldClearCache() {
        when(carRepository.save(car)).thenReturn(car);
        Car created = carService.createCar(car);
        assertThat(created).isEqualTo(car);
        verify(searchCache).clear();
    }

    @Test
    void updateCar_shouldClearCache() {
        Car updatedDetails = new Car();
        updatedDetails.setBrand("Audi");
        updatedDetails.setModel("Q7");
        updatedDetails.setYear(2024);
        updatedDetails.setPrice(70000.0);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(selfProvider.getObject()).thenReturn(carService);
        Car result = carService.updateCar(1L, updatedDetails);
        assertThat(result.getBrand()).isEqualTo("Audi");
        verify(searchCache).clear();
    }

    @Test
    void deleteCar_shouldClearCache_andThrowIfSold() {
        Sale sale = new Sale();
        sale.setId(10L);
        sale.setSaleDate(LocalDateTime.now());
        car.setSale(sale);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(selfProvider.getObject()).thenReturn(carService);

        assertThatThrownBy(() -> carService.deleteCar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже продана");
        verify(searchCache, never()).clear();
    }

    @Test
    void createCarsBulk_shouldFilterDuplicates() {
        List<CarRequestDto> requests = List.of(requestDto, requestDto);
        when(carMapper.toEntity(requestDto)).thenReturn(car);
        when(carRepository.findAll()).thenReturn(List.of()); // нет в БД
        when(carRepository.saveAll(anyList())).thenReturn(List.of(car));
        when(carMapper.toResponseDto(car)).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulk(requests);
        assertThat(result).hasSize(1); // дубликат отфильтрован
        verify(carRepository, times(1)).saveAll(anyList());
        verify(searchCache).clear();
    }
}