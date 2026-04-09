# 5 ЛИСТИНГ КОДА

Файл `src/main/java/com/example/autosalon/dto/CarListRequestDto.java`

```java
package com.example.autosalon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CarListRequestDto {
    @NotEmpty(message = "Список автомобилей не может быть пустым")
    private List<@Valid CarRequestDto> cars;
}
```

Файл `src/main/java/com/example/autosalon/controller/CarController.java`

```java
@PostMapping("/bulk")
@Operation(summary = "Массовое создание автомобилей",
        description = "Создает несколько автомобилей за один запрос")
public ResponseEntity<List<CarResponseDto>> createCarsBulk(
        @Valid @RequestBody CarListRequestDto bulkDto) {
    List<CarResponseDto> created = carService.createCarsBulk(bulkDto.getCars());
    return new ResponseEntity<>(created, HttpStatus.CREATED);
}

@PostMapping("/bulk/transactional")
@Operation(summary = "Массовое создание с транзакцией (откат при ошибке)")
public ResponseEntity<List<CarResponseDto>> createCarsBulkTransactional(
        @Valid @RequestBody CarListRequestDto bulkDto) {
    List<CarResponseDto> result = carService.createCarsBulkTransactional(bulkDto.getCars());
    return new ResponseEntity<>(result, HttpStatus.CREATED);
}

@PostMapping("/bulk/non-transactional")
@Operation(summary = "Массовое создание без транзакции (частичное сохранение)")
public ResponseEntity<List<CarResponseDto>> createCarsBulkNonTransactional(
        @Valid @RequestBody CarListRequestDto bulkDto) {
    List<CarResponseDto> result = carService.createCarsBulkNonTransactional(bulkDto.getCars());
    return new ResponseEntity<>(result, HttpStatus.CREATED);
}
```

Файл `src/main/java/com/example/autosalon/service/CarService.java`

```java
@Transactional
public List<CarResponseDto> createCarsBulk(List<CarRequestDto> carRequests) {
    log.info("Массовое создание автомобилей: получено {} записей", carRequests.size());

    Set<String> seenKeys = new HashSet<>();
    List<CarRequestDto> uniqueRequests = carRequests.stream()
            .filter(dto -> {
                String key = dto.getBrand().toLowerCase() + "|" +
                        dto.getModel().toLowerCase() + "|" +
                        dto.getYear();
                if (seenKeys.contains(key)) {
                    log.warn("Дубликат в пакете: {} {} {}, пропускаем",
                            dto.getBrand(), dto.getModel(), dto.getYear());
                    return false;
                }
                seenKeys.add(key);
                return true;
            })
            .toList();

    List<Car> existingCars = carRepository.findAll();

    List<Car> carsToSave = uniqueRequests.stream()
            .map(carMapper::toEntity)
            .filter(newCar -> {
                Optional<Car> existing = existingCars.stream()
                        .filter(c -> c.getBrand().equalsIgnoreCase(newCar.getBrand())
                                && c.getModel().equalsIgnoreCase(newCar.getModel())
                                && c.getYear() == newCar.getYear())
                        .findFirst();
                if (existing.isPresent()) {
                    log.info("Машина {} {} {} уже существует в БД (ID={}), пропускаем",
                            newCar.getBrand(), newCar.getModel(), newCar.getYear(),
                            existing.get().getId());
                    return false;
                }
                return true;
            })
            .toList();

    if (carsToSave.isEmpty()) {
        log.info("Нет новых автомобилей для сохранения (все дубликаты)");
        return List.of();
    }

    List<Car> savedCars = carRepository.saveAll(carsToSave);
    searchCache.clear();

    return savedCars.stream()
            .map(carMapper::toResponseDto)
            .toList();
}

@Transactional
public List<CarResponseDto> createCarsBulkTransactional(List<CarRequestDto> carRequests) {
    log.info("=== ТРАНЗАКЦИОННЫЙ режим: получено {} записей ===", carRequests.size());

    List<Car> carsToSave = carRequests.stream()
            .map(carMapper::toEntity)
            .toList();

    for (int i = 0; i < carsToSave.size(); i++) {
        if (i == 2) {
            String errorMsg = String.format("Симуляция ошибки на автомобиле #%d: %s %s %d",
                    i + 1,
                    carsToSave.get(i).getBrand(),
                    carsToSave.get(i).getModel(),
                    carsToSave.get(i).getYear());
            throw new IllegalStateException(errorMsg);
        }
    }

    List<Car> saved = carRepository.saveAll(carsToSave);
    searchCache.clear();
    return saved.stream().map(carMapper::toResponseDto).toList();
}

public List<CarResponseDto> createCarsBulkNonTransactional(List<CarRequestDto> carRequests) {
    log.info("=== НЕТРАНЗАКЦИОННЫЙ режим: получено {} записей ===", carRequests.size());

    List<Car> carsToSave = carRequests.stream()
            .map(carMapper::toEntity)
            .toList();

    List<Car> saved = new ArrayList<>();

    for (int i = 0; i < carsToSave.size(); i++) {
        if (i == 2) {
            String errorMsg = String.format("Ошибка на автомобиле #%d: %s %s %d – предыдущие уже сохранены!",
                    i + 1,
                    carsToSave.get(i).getBrand(),
                    carsToSave.get(i).getModel(),
                    carsToSave.get(i).getYear());
            throw new IllegalStateException(errorMsg);
        }
        Car savedCar = carRepository.save(carsToSave.get(i));
        saved.add(savedCar);
    }

    searchCache.clear();
    return saved.stream().map(carMapper::toResponseDto).toList();
}
```

Файл `src/test/java/com/example/autosalon/service/CarServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock private CarRepository carRepository;
    @Mock private CarMapper carMapper;
    @Mock private CarSearchCache searchCache;
    @Mock private ObjectProvider<CarService> selfProvider;
    @InjectMocks private CarService carService;

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
    }

    @Test
    void createCarsBulkTransactional_shouldRollbackOnError() {
        List<CarRequestDto> requests = List.of(requestDto, requestDto, requestDto);
        when(carMapper.toEntity(any(CarRequestDto.class))).thenReturn(car);

        assertThatThrownBy(() -> carService.createCarsBulkTransactional(requests))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Симуляция ошибки");

        verify(carRepository, never()).saveAll(anyList());
    }

    @Test
    void createCarsBulkNonTransactional_shouldSavePartialOnError() {
        List<CarRequestDto> requests = List.of(dto1, dto2, dto3);

        when(carMapper.toEntity(dto1)).thenReturn(firstCar);
        when(carMapper.toEntity(dto2)).thenReturn(secondCar);
        when(carMapper.toEntity(dto3)).thenReturn(thirdCar);
        when(carRepository.save(firstCar)).thenReturn(firstCar);
        when(carRepository.save(secondCar)).thenReturn(secondCar);

        assertThatThrownBy(() -> carService.createCarsBulkNonTransactional(requests))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ошибка на автомобиле #3");

        verify(carRepository, times(2)).save(any(Car.class));
        verify(carRepository, never()).save(thirdCar);
    }
}
```

Файл `src/test/java/com/example/autosalon/service/DealershipServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class DealershipServiceTest {

    @Mock private DealershipRepository dealershipRepository;
    @Mock private CarRepository carRepository;
    @Mock private CarService carService;
    @Mock private DealershipTransactionalService dealershipTransactionalService;
    @InjectMocks private DealershipService dealershipService;

    @Test
    void addCarToDealership_shouldAdd() {
        when(dealershipRepository.findByIdWithCars(1L)).thenReturn(Optional.of(dealership));
        when(carRepository.findById(10L)).thenReturn(Optional.of(car));

        Dealership updated = dealershipService.addCarToDealership(1L, 10L);

        assertThat(updated.getCars()).contains(car);
        assertThat(car.getDealership()).isEqualTo(dealership);
    }
}
```

Файл `src/test/java/com/example/autosalon/service/SaleServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private CarService carService;
    @Mock private CustomerService customerService;
    @InjectMocks private SaleService saleService;

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
}
```
