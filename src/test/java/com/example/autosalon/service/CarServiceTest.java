package com.example.autosalon.service;

import com.example.autosalon.cache.CarCacheKey;
import com.example.autosalon.cache.CarSearchCache;
import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.CarSearchRequest;
import com.example.autosalon.dto.PageResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.SaleRepository;
import com.example.autosalon.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock private CarRepository carRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CarMapper carMapper;
    @Mock private CarSearchCache searchCache;
    @Mock private ObjectProvider<CarService> selfProvider;
    @Mock private CarImageService carImageService;
    @InjectMocks private CarService carService;

    private Car car;
    private CarRequestDto requestDto;
    private CarResponseDto responseDto;
    private UserAccount owner;

    @BeforeEach
    void setUp() {
        car = new Car();
        car.setId(1L);
        car.setBrand("BMW");
        car.setModel("X5");
        car.setYear(2023);
        car.setColor("Чёрный");
        car.setInteriorColor("Чёрный");
        car.setInteriorMaterial("Кожа");
        car.setEngineVolume(3.0);
        car.setMileage(10000);
        car.setPowerHp(340);
        car.setFuelConsumptionCity(11.0);
        car.setFuelConsumptionHighway(7.0);
        car.setFuelConsumptionMixed(8.5);
        car.setSeatCount(5);
        car.setCity("Москва");
        car.setTransmission("auto");
        car.setBodyType("sedan");
        car.setEngineType("petrol");
        car.setDriveType("awd");
        car.setPrice(60000.0);
        owner = new UserAccount();
        owner.setId(77L);
        car.setOwner(owner);

        requestDto = new CarRequestDto();
        requestDto.setBrand("BMW");
        requestDto.setModel("X5");
        requestDto.setYear(2023);
        requestDto.setColor("Чёрный");
        requestDto.setInteriorColor("Чёрный");
        requestDto.setInteriorMaterial("Кожа");
        requestDto.setEngineVolume(3.0);
        requestDto.setMileage(10000);
        requestDto.setPowerHp(340);
        requestDto.setFuelConsumptionCity(11.0);
        requestDto.setFuelConsumptionHighway(7.0);
        requestDto.setFuelConsumptionMixed(8.5);
        requestDto.setSeatCount(5);
        requestDto.setCity("Москва");
        requestDto.setTransmission("auto");
        requestDto.setBodyType("sedan");
        requestDto.setEngineType("petrol");
        requestDto.setDriveType("awd");
        requestDto.setPrice(60000.0);

        responseDto = new CarResponseDto();
        responseDto.setId(1L);
        responseDto.setBrand("BMW");
        responseDto.setModel("X5");
        responseDto.setYear(2023);
        responseDto.setColor("Чёрный");
        responseDto.setPrice(60000.0);
    }

    // ========== Существующие тесты (оставляем) ==========

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
        Car created = carService.createCar(car, owner, null);
        assertThat(created).isEqualTo(car);
        verify(searchCache).clear();
    }

    @Test
    void updateCar_shouldClearCache() {
        Car updatedDetails = new Car();
        updatedDetails.setBrand("Audi");
        updatedDetails.setModel("Q7");
        updatedDetails.setYear(2024);
        updatedDetails.setColor("Белый");
        updatedDetails.setInteriorColor("Бежевый");
        updatedDetails.setInteriorMaterial("Кожа");
        updatedDetails.setEngineVolume(3.0);
        updatedDetails.setMileage(5000);
        updatedDetails.setPowerHp(340);
        updatedDetails.setFuelConsumptionCity(10.5);
        updatedDetails.setFuelConsumptionHighway(6.5);
        updatedDetails.setFuelConsumptionMixed(8.0);
        updatedDetails.setSeatCount(5);
        updatedDetails.setCity("Санкт-Петербург");
        updatedDetails.setTransmission("auto");
        updatedDetails.setBodyType("suv");
        updatedDetails.setEngineType("diesel");
        updatedDetails.setDriveType("awd");
        updatedDetails.setPrice(70000.0);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(selfProvider.getObject()).thenReturn(carService);

        Car result = carService.updateCar(1L, updatedDetails, owner);

        assertThat(result.getBrand()).isEqualTo("Audi");
        assertThat(result.getModel()).isEqualTo("Q7");
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getColor()).isEqualTo("Белый");
        assertThat(result.getCity()).isEqualTo("Санкт-Петербург");
        assertThat(result.getPowerHp()).isEqualTo(340);
        assertThat(result.getPrice()).isEqualTo(70000.0);
        verify(searchCache).clear();
    }

    @Test
    void deleteCar_shouldThrowIfSold() {
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
    void deleteCar_shouldThrowIfSold_withKnownCustomerInMessage() {
        Sale sale = new Sale();
        sale.setId(10L);
        sale.setSaleDate(LocalDateTime.now());
        Customer customer = new Customer();
        customer.setFirstName("Ivan");
        customer.setLastName("Petrov");
        sale.setCustomer(customer);
        car.setSale(sale);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(selfProvider.getObject()).thenReturn(carService);

        assertThatThrownBy(() -> carService.deleteCar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ivan")
                .hasMessageContaining("Petrov");
    }

    @Test
    void deleteCar_shouldDeleteAndClearCache_whenNotSold() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(selfProvider.getObject()).thenReturn(carService);
        doNothing().when(carRepository).delete(car);

        carService.deleteCar(1L);

        verify(carImageService).deleteAllFilesForCar(1L);
        verify(carRepository).delete(car);
        verify(searchCache).clear();
    }

    @Test
    void createCarsBulk_shouldFilterDuplicates() {
        List<CarRequestDto> requests = List.of(requestDto, requestDto);
        when(carMapper.toEntity(requestDto)).thenReturn(car);
        when(carRepository.findAll()).thenReturn(List.of());
        when(carRepository.saveAll(anyList())).thenReturn(List.of(car));
        when(carMapper.toResponseDto(car)).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulk(requests);

        assertThat(result).hasSize(1);
        verify(carRepository, times(1)).saveAll(anyList());
        verify(searchCache).clear();
    }

    // ========== НОВЫЕ ТЕСТЫ ==========

    @Test
    void getAllCars_shouldReturnAllWithRelations() {
        when(carRepository.findAllWithAllRelations()).thenReturn(List.of(car));

        List<Car> result = carService.getAllCars();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(car);
        verify(carRepository).findAllWithAllRelations();
    }

    @Test
    void getCarsByBrand_shouldReturnFilteredCars() {
        when(carRepository.findByBrandIgnoreCase("BMW")).thenReturn(List.of(car));

        List<Car> result = carService.getCarsByBrand("BMW");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBrand()).isEqualTo("BMW");
        verify(carRepository).findByBrandIgnoreCase("BMW");
    }

    @Test
    void getCarsByBrand_shouldReturnEmptyList_whenNoMatch() {
        when(carRepository.findByBrandIgnoreCase("Unknown")).thenReturn(List.of());

        List<Car> result = carService.getCarsByBrand("Unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void getCarsByFeatureCategoryJpql_shouldReturnCars() {
        when(carRepository.findCarsByFeatureCategoryJpql("Комфорт")).thenReturn(List.of(car));

        List<Car> result = carService.getCarsByFeatureCategoryJpql("Комфорт");

        assertThat(result).hasSize(1);
        verify(carRepository).findCarsByFeatureCategoryJpql("Комфорт");
    }

    @Test
    void createCarsBulk_shouldSkipExistingInDatabase() {
        Car existingCar = new Car();
        existingCar.setId(2L);
        existingCar.setBrand("BMW");
        existingCar.setModel("X5");
        existingCar.setYear(2023);

        List<CarRequestDto> requests = List.of(requestDto);

        when(carMapper.toEntity(requestDto)).thenReturn(car);
        when(carRepository.findAll()).thenReturn(List.of(existingCar));

        List<CarResponseDto> result = carService.createCarsBulk(requests);

        assertThat(result).isEmpty();
        verify(carRepository, never()).saveAll(anyList());
        verify(searchCache, never()).clear();
    }

    @Test
    void createCarsBulk_shouldHandleEmptyList() {
        List<CarResponseDto> result = carService.createCarsBulk(List.of());

        assertThat(result).isEmpty();
        verify(carRepository, never()).saveAll(any());
    }

    @Test
    void createCarsBulk_shouldFilterPackageDuplicates() {
        CarRequestDto duplicate1 = new CarRequestDto();
        duplicate1.setBrand("Toyota");
        duplicate1.setModel("Camry");
        duplicate1.setYear(2022);
        duplicate1.setColor("Красный");
        duplicate1.setInteriorColor("Чёрный");
        duplicate1.setInteriorMaterial("Ткань");
        duplicate1.setEngineVolume(2.5);
        duplicate1.setMileage(20000);
        duplicate1.setPowerHp(200);
        duplicate1.setFuelConsumptionCity(9.0);
        duplicate1.setFuelConsumptionHighway(5.5);
        duplicate1.setFuelConsumptionMixed(7.0);
        duplicate1.setSeatCount(5);
        duplicate1.setCity("Минск");
        duplicate1.setTransmission("auto");
        duplicate1.setBodyType("sedan");
        duplicate1.setEngineType("petrol");
        duplicate1.setDriveType("fwd");
        duplicate1.setPrice(30000.0);

        CarRequestDto duplicate2 = new CarRequestDto();
        duplicate2.setBrand("Toyota");
        duplicate2.setModel("Camry");
        duplicate2.setYear(2022);
        duplicate2.setColor("Синий");
        duplicate2.setInteriorColor("Серый");
        duplicate2.setInteriorMaterial("Ткань");
        duplicate2.setEngineVolume(2.5);
        duplicate2.setMileage(21000);
        duplicate2.setPowerHp(200);
        duplicate2.setFuelConsumptionCity(9.0);
        duplicate2.setFuelConsumptionHighway(5.5);
        duplicate2.setFuelConsumptionMixed(7.0);
        duplicate2.setSeatCount(5);
        duplicate2.setCity("Минск");
        duplicate2.setTransmission("auto");
        duplicate2.setBodyType("sedan");
        duplicate2.setEngineType("petrol");
        duplicate2.setDriveType("fwd");
        duplicate2.setPrice(31000.0);

        List<CarRequestDto> requests = List.of(duplicate1, duplicate2);

        Car car1 = new Car();
        car1.setBrand("Toyota");
        car1.setModel("Camry");
        car1.setYear(2022);

        when(carMapper.toEntity(duplicate1)).thenReturn(car1);
        when(carRepository.findAll()).thenReturn(List.of());
        when(carRepository.saveAll(anyList())).thenReturn(List.of(car1));
        when(carMapper.toResponseDto(car1)).thenReturn(new CarResponseDto());

        List<CarResponseDto> result = carService.createCarsBulk(requests);

        assertThat(result).hasSize(1); // только один из дубликатов
    }

    @Test
    void createCarsBulk_shouldSave_whenExistingCarsDifferByModelOrYear() {
        CarRequestDto incoming = createRequestDto("BMW", "X5", 2023);
        Car mapped = new Car();
        mapped.setBrand("BMW");
        mapped.setModel("X5");
        mapped.setYear(2023);

        Car existingDifferentModel = new Car();
        existingDifferentModel.setId(100L);
        existingDifferentModel.setBrand("BMW");
        existingDifferentModel.setModel("X3");
        existingDifferentModel.setYear(2023);

        Car existingDifferentYear = new Car();
        existingDifferentYear.setId(101L);
        existingDifferentYear.setBrand("BMW");
        existingDifferentYear.setModel("X5");
        existingDifferentYear.setYear(2022);

        Car existingDifferentBrand = new Car();
        existingDifferentBrand.setId(102L);
        existingDifferentBrand.setBrand("Audi");
        existingDifferentBrand.setModel("A6");
        existingDifferentBrand.setYear(2023);

        when(carMapper.toEntity(incoming)).thenReturn(mapped);
        when(carRepository.findAll())
                .thenReturn(List.of(existingDifferentBrand, existingDifferentModel, existingDifferentYear));
        when(carRepository.saveAll(anyList())).thenReturn(List.of(mapped));
        when(carMapper.toResponseDto(mapped)).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulk(List.of(incoming));

        assertThat(result).hasSize(1);
        verify(carRepository).saveAll(anyList());
    }

    @Test
    void createCarsBulkTransactional_shouldThrowConflict_whenDuplicateExistsInDb() {
        List<CarRequestDto> requests = List.of(requestDto);
        when(carMapper.toEntity(requestDto)).thenReturn(car);
        when(carRepository.findAll()).thenReturn(List.of(car));

        assertThatThrownBy(() -> carService.createCarsBulkTransactional(requests))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже существует в БД");

        verify(carRepository, never()).saveAll(anyList());
        verify(searchCache, never()).clear();
    }

    @Test
    void createCarsBulkTransactional_shouldSaveAll_whenNoError() {
        List<CarRequestDto> requests = List.of(requestDto, requestDto);

        Car car2 = new Car();
        car2.setBrand("BMW");
        car2.setModel("X5");
        car2.setYear(2023);

        when(carMapper.toEntity(any(CarRequestDto.class))).thenReturn(car, car2);
        when(carRepository.findAll()).thenReturn(List.of());
        when(carRepository.saveAll(anyList())).thenReturn(List.of(car, car2));
        when(carMapper.toResponseDto(any(Car.class))).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulkTransactional(requests);

        assertThat(result).hasSize(2);
        verify(carRepository).saveAll(anyList());
        verify(searchCache).clear();
    }

    @Test
    void createCarsBulkTransactional_shouldSave_whenExistingCarsDifferByModelOrYear() {
        CarRequestDto incoming = createRequestDto("BMW", "X5", 2023);
        Car mapped = new Car();
        mapped.setBrand("BMW");
        mapped.setModel("X5");
        mapped.setYear(2023);

        Car existingDifferentModel = new Car();
        existingDifferentModel.setBrand("BMW");
        existingDifferentModel.setModel("X3");
        existingDifferentModel.setYear(2023);

        Car existingDifferentYear = new Car();
        existingDifferentYear.setBrand("BMW");
        existingDifferentYear.setModel("X5");
        existingDifferentYear.setYear(2022);

        when(carMapper.toEntity(incoming)).thenReturn(mapped);
        when(carRepository.findAll()).thenReturn(List.of(existingDifferentModel, existingDifferentYear));
        when(carRepository.saveAll(anyList())).thenReturn(List.of(mapped));
        when(carMapper.toResponseDto(mapped)).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulkTransactional(List.of(incoming));

        assertThat(result).hasSize(1);
        verify(carRepository).saveAll(anyList());
        verify(searchCache).clear();
    }

    @Test
    void createCarsBulkTransactional_shouldSave_whenExistingCarHasDifferentBrand() {
        CarRequestDto incoming = createRequestDto("BMW", "X5", 2023);
        Car mapped = new Car();
        mapped.setBrand("BMW");
        mapped.setModel("X5");
        mapped.setYear(2023);

        Car existingDifferentBrand = new Car();
        existingDifferentBrand.setBrand("Audi");
        existingDifferentBrand.setModel("X5");
        existingDifferentBrand.setYear(2023);

        when(carMapper.toEntity(incoming)).thenReturn(mapped);
        when(carRepository.findAll()).thenReturn(List.of(existingDifferentBrand));
        when(carRepository.saveAll(anyList())).thenReturn(List.of(mapped));
        when(carMapper.toResponseDto(mapped)).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulkTransactional(List.of(incoming));

        assertThat(result).hasSize(1);
        verify(carRepository).saveAll(anyList());
        verify(searchCache).clear();
    }

    @Test
    void createCarsBulkNonTransactional_shouldSavePartialBeforeConflict() {
        CarRequestDto dto1 = createRequestDto("BMW", "X5", 2023);
        CarRequestDto dto2 = createRequestDto("Audi", "Q7", 2022);
        CarRequestDto dto3 = createRequestDto("CCC", "C3", 2022);

        Car firstCar = new Car();
        firstCar.setBrand("BMW");
        firstCar.setModel("X5");
        firstCar.setYear(2023);

        Car secondCar = new Car();
        secondCar.setBrand("Audi");
        secondCar.setModel("Q7");
        secondCar.setYear(2022);

        Car thirdCar = new Car();
        thirdCar.setBrand("CCC");
        thirdCar.setModel("C3");
        thirdCar.setYear(2022);

        Car existing = new Car();
        existing.setId(100L);
        existing.setBrand("CCC");
        existing.setModel("C3");
        existing.setYear(2022);

        when(carMapper.toEntity(dto1)).thenReturn(firstCar);
        when(carMapper.toEntity(dto2)).thenReturn(secondCar);
        when(carMapper.toEntity(dto3)).thenReturn(thirdCar);
        when(carRepository.findAll()).thenReturn(List.of(existing));
        when(carRepository.save(firstCar)).thenReturn(firstCar);
        when(carRepository.save(secondCar)).thenReturn(secondCar);

        // Fix: Create the list outside the lambda
        List<CarRequestDto> requests = List.of(dto1, dto2, dto3);

        assertThatThrownBy(() -> carService.createCarsBulkNonTransactional(requests))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже существует в БД");

        verify(carRepository, times(2)).save(any(Car.class));
        verify(carRepository, never()).save(thirdCar);
        verify(searchCache, never()).clear();
    }

    @Test
    void createCarsBulkNonTransactional_shouldSaveAll_whenNoError() {
        CarRequestDto secondRequest = createRequestDto("Audi", "Q7", 2023);
        List<CarRequestDto> requests = List.of(requestDto, secondRequest);

        Car car2 = new Car();
        car2.setBrand("Audi");
        car2.setModel("Q7");
        car2.setYear(2023);

        when(carMapper.toEntity(requestDto)).thenReturn(car);
        when(carMapper.toEntity(secondRequest)).thenReturn(car2);
        when(carRepository.findAll()).thenReturn(List.of());
        when(carRepository.save(any(Car.class))).thenReturn(car, car2);
        when(carMapper.toResponseDto(any(Car.class))).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulkNonTransactional(requests);

        assertThat(result).hasSize(2);
        verify(carRepository, times(2)).save(any(Car.class));
        verify(searchCache).clear();
    }

    @Test
    void createCarsBulkNonTransactional_shouldSave_whenExistingCarsDifferByModelOrYear() {
        CarRequestDto incoming = createRequestDto("BMW", "X5", 2023);
        Car mapped = new Car();
        mapped.setBrand("BMW");
        mapped.setModel("X5");
        mapped.setYear(2023);

        Car existingDifferentModel = new Car();
        existingDifferentModel.setBrand("BMW");
        existingDifferentModel.setModel("X3");
        existingDifferentModel.setYear(2023);

        Car existingDifferentYear = new Car();
        existingDifferentYear.setBrand("BMW");
        existingDifferentYear.setModel("X5");
        existingDifferentYear.setYear(2022);

        when(carMapper.toEntity(incoming)).thenReturn(mapped);
        when(carRepository.findAll()).thenReturn(List.of(existingDifferentModel, existingDifferentYear));
        when(carRepository.save(mapped)).thenReturn(mapped);
        when(carMapper.toResponseDto(mapped)).thenReturn(responseDto);

        List<CarResponseDto> result = carService.createCarsBulkNonTransactional(List.of(incoming));

        assertThat(result).hasSize(1);
        verify(carRepository).save(mapped);
        verify(searchCache).clear();
    }

    @Test
    void findCarsWithPaginationJpql_shouldReturnFromCache_whenExists() {
        CarSearchRequest request = new CarSearchRequest();
        request.setFeatureCategory("Комфорт");
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("id");
        request.setSortDirection(Sort.Direction.ASC);

        PageResponseDto<CarResponseDto> cachedResponse = PageResponseDto.<CarResponseDto>builder()
                .content(List.of(responseDto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(searchCache.get(any(CarCacheKey.class))).thenReturn(cachedResponse);

        PageResponseDto<CarResponseDto> result = carService.findCarsWithPaginationJpql(request);

        assertThat(result).isEqualTo(cachedResponse);
        verify(carRepository, never()).findCarsByFeatureCategoryWithPagination(any(), any());
    }

    @Test
    void findCarsWithPaginationJpql_shouldSaveToCache_whenNotExists() {
        CarSearchRequest request = new CarSearchRequest();
        request.setFeatureCategory("Комфорт");
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("id");
        request.setSortDirection(Sort.Direction.ASC);

        Page<Car> carPage = new PageImpl<>(List.of(car), PageRequest.of(0, 10), 1);

        when(searchCache.get(any(CarCacheKey.class))).thenReturn(null);
        when(carRepository.findCarsByFeatureCategoryWithPagination(eq("Комфорт"), any(Pageable.class)))
                .thenReturn(carPage);
        when(carMapper.toResponseDto(car)).thenReturn(responseDto);

        PageResponseDto<CarResponseDto> result = carService.findCarsWithPaginationJpql(request);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(searchCache).put(any(CarCacheKey.class), any(PageResponseDto.class));
    }

    @Test
    void findCarsWithPaginationJpql_shouldHandleNullFeatureCategory() {
        CarSearchRequest request = new CarSearchRequest();
        request.setFeatureCategory(null);
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("id");
        request.setSortDirection(Sort.Direction.ASC);

        Page<Car> carPage = new PageImpl<>(List.of(car), PageRequest.of(0, 10), 1);

        when(searchCache.get(any(CarCacheKey.class))).thenReturn(null);
        when(carRepository.findCarsByFeatureCategoryWithPagination(eq(null), any(Pageable.class)))
                .thenReturn(carPage);
        when(carMapper.toResponseDto(car)).thenReturn(responseDto);

        PageResponseDto<CarResponseDto> result = carService.findCarsWithPaginationJpql(request);

        assertThat(result).isNotNull();
        verify(searchCache).put(any(CarCacheKey.class), any(PageResponseDto.class));
    }

    @Test
    void findCarsWithPaginationJpql_shouldHandleEmptyResult() {
        CarSearchRequest request = new CarSearchRequest();
        request.setFeatureCategory("Несуществующая");
        request.setPage(0);
        request.setSize(10);

        Page<Car> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(searchCache.get(any(CarCacheKey.class))).thenReturn(null);
        when(carRepository.findCarsByFeatureCategoryWithPagination(eq("Несуществующая"), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponseDto<CarResponseDto> result = carService.findCarsWithPaginationJpql(request);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(searchCache).put(any(CarCacheKey.class), any(PageResponseDto.class));
    }

    @Test
    void mapToPageResponse_shouldConvertCorrectly() {
        Page<Car> carPage = new PageImpl<>(List.of(car), PageRequest.of(2, 15), 45);
        when(searchCache.get(any())).thenReturn(null);
        when(carRepository.findCarsByFeatureCategoryWithPagination(any(), any(Pageable.class)))
                .thenReturn(carPage);
        when(carMapper.toResponseDto(car)).thenReturn(responseDto);

        PageResponseDto<CarResponseDto> result =
                carService.findCarsWithPaginationJpql(createRequestWithPage(2, 15));

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(15);
        assertThat(result.getTotalElements()).isEqualTo(45);
        assertThat(result.getTotalPages()).isEqualTo(3); // 45/15 = 3
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue(); // page 2 из 3 (0,1,2) -> последняя
    }

    private CarSearchRequest createRequestWithPage(int page, int size) {
        CarSearchRequest request = new CarSearchRequest();
        request.setFeatureCategory("Комфорт");
        request.setPage(page);
        request.setSize(size);
        request.setSortBy("id");
        request.setSortDirection(Sort.Direction.ASC);
        return request;
    }

    @Test
    void createDealershipCarBulk_assignsOwnerAndClearsCache() {
        CarRequestDto a = createRequestDto("Audi", "A4", 2022);
        CarRequestDto b = createRequestDto("VW", "Golf", 2021);

        Car entityA = new Car();
        entityA.setBrand("Audi");
        Car entityB = new Car();
        entityB.setBrand("VW");

        when(carMapper.toEntity(a)).thenReturn(entityA);
        when(carMapper.toEntity(b)).thenReturn(entityB);

        Car savedA = new Car();
        savedA.setId(10L);
        Car savedB = new Car();
        savedB.setId(11L);

        when(carRepository.saveAll(anyList())).thenReturn(List.of(savedA, savedB));

        CarResponseDto dtoA = new CarResponseDto();
        dtoA.setId(10L);
        CarResponseDto dtoB = new CarResponseDto();
        dtoB.setId(11L);
        when(carMapper.toResponseDto(savedA)).thenReturn(dtoA);
        when(carMapper.toResponseDto(savedB)).thenReturn(dtoB);

        List<CarResponseDto> result = carService.createDealershipCarBulk(owner, List.of(a, b));

        assertThat(result).hasSize(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Car>> captor = ArgumentCaptor.forClass(List.class);
        verify(carRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getOwner()).isSameAs(owner);
        assertThat(captor.getValue().get(1).getOwner()).isSameAs(owner);
        assertThat(captor.getValue().get(0).getId()).isNull();
        verify(searchCache).clear();
    }

    @Test
    void createDealershipCarBulk_throwsWhenDuplicateInBatch() {
        CarRequestDto a = createRequestDto("BMW", "X5", 2023);
        CarRequestDto b = createRequestDto("bmw", "x5", 2023);

        assertThatThrownBy(() -> carService.createDealershipCarBulk(owner, List.of(a, b)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("повторяется");
    }

    private CarRequestDto createRequestDto(String brand, String model, int year) {
        CarRequestDto dto = new CarRequestDto();
        dto.setBrand(brand);
        dto.setModel(model);
        dto.setYear(year);
        dto.setColor("Чёрный");
        dto.setInteriorColor("Чёрный");
        dto.setInteriorMaterial("Кожа");
        dto.setEngineVolume(2.0);
        dto.setMileage(15000);
        dto.setPowerHp(200);
        dto.setFuelConsumptionCity(9.5);
        dto.setFuelConsumptionHighway(6.0);
        dto.setFuelConsumptionMixed(7.5);
        dto.setSeatCount(5);
        dto.setCity("Москва");
        dto.setTransmission("auto");
        dto.setBodyType("sedan");
        dto.setEngineType("petrol");
        dto.setDriveType("fwd");
        dto.setPrice(50000.0);
        return dto;
    }
}