package com.example.autosalon.service;

import com.example.autosalon.cache.CarCacheKey;
import com.example.autosalon.cache.CarSearchCache;
import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.CarSearchRequest;
import com.example.autosalon.dto.PageResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final SaleRepository saleRepository;
    private final CarMapper carMapper;
    private final CarSearchCache searchCache;
    private final ObjectProvider<CarService> self;

    @Transactional(readOnly = true)
    public List<Car> getAllCars() {
        return carRepository.findAllWithAllRelations();
    }

    @Transactional(readOnly = true)
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + id + " не найдена"));

    }

    @Transactional
    public Car createCar(Car car) {
        car.setId(null);
        Car saved = carRepository.save(car);
        searchCache.clear();
        log.info(" Создана новая машина, кэш очищен");
        return saved;
    }

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

        log.info("Успешно создано {} автомобилей (пропущено дубликатов: {})",
                savedCars.size(), carRequests.size() - carsToSave.size());

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

        List<Car> existingCars = carRepository.findAll();
        for (Car newCar : carsToSave) {
            boolean duplicateExists = existingCars.stream()
                    .anyMatch(existing -> existing.getBrand().equalsIgnoreCase(newCar.getBrand())
                            && existing.getModel().equalsIgnoreCase(newCar.getModel())
                            && existing.getYear() == newCar.getYear());
            if (duplicateExists) {
                String errorMsg = String.format(
                        "Конфликт данных: автомобиль %s %s %d уже существует в БД",
                        newCar.getBrand(),
                        newCar.getModel(),
                        newCar.getYear()
                );
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        }

        List<Car> saved = carRepository.saveAll(carsToSave);
        searchCache.clear();
        log.info("Транзакционный режим: успешно сохранено {} автомобилей", saved.size());
        return saved.stream().map(carMapper::toResponseDto).toList();
    }

    public List<CarResponseDto> createCarsBulkNonTransactional(List<CarRequestDto> carRequests) {
        log.info("=== НЕТРАНЗАКЦИОННЫЙ режим: получено {} записей ===", carRequests.size());

        List<Car> carsToSave = carRequests.stream()
                .map(carMapper::toEntity)
                .toList();

        List<Car> knownCars = new ArrayList<>(carRepository.findAll());
        List<Car> saved = new ArrayList<>();

        for (Car carToSave : carsToSave) {
            boolean duplicateExists = knownCars.stream()
                    .anyMatch(existing -> existing.getBrand().equalsIgnoreCase(carToSave.getBrand())
                            && existing.getModel().equalsIgnoreCase(carToSave.getModel())
                            && existing.getYear() == carToSave.getYear());
            if (duplicateExists) {
                String errorMsg = String.format(
                        "Конфликт данных: автомобиль %s %s %d уже существует в БД",
                        carToSave.getBrand(),
                        carToSave.getModel(),
                        carToSave.getYear()
                );
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            Car savedCar = carRepository.save(carToSave);
            saved.add(savedCar);
            knownCars.add(savedCar);
        }

        searchCache.clear();
        log.info("Нетранзакционный режим: успешно сохранено {} автомобилей", saved.size());
        return saved.stream().map(carMapper::toResponseDto).toList();
    }

    @Transactional
    public Car updateCar(Long id, Car carDetails) {
        Car existingCar = self.getObject().getCarById(id);

        existingCar.setBrand(carDetails.getBrand());
        existingCar.setModel(carDetails.getModel());
        existingCar.setYear(carDetails.getYear());
        existingCar.setColor(carDetails.getColor());
        existingCar.setPrice(carDetails.getPrice());

        searchCache.clear();
        log.info(" Машина обновлена, кэш очищен");

        return existingCar;
    }

    @Transactional
    public void deleteCar(Long id) {
        Car car = self.getObject().getCarById(id);

        if (car.getSale() != null) {
            Sale sale = car.getSale();
            String errorMessage = String.format(
                    "Невозможно удалить машину ID=%d %s %s %d - она уже продана! "
                            + "(ID продажи: %d, дата продажи: %s, покупатель: %s %s)",
                    car.getId(),
                    car.getBrand(),
                    car.getModel(),
                    car.getYear(),
                    sale.getId(),
                    sale.getSaleDate().toLocalDate(),
                    sale.getCustomer() != null ? sale.getCustomer().getFirstName() : "неизвестно",
                    sale.getCustomer() != null ? sale.getCustomer().getLastName() : "неизвестно"
            );

            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("Удаление машины ID={} {} {} (не продана)",
                car.getId(), car.getBrand(), car.getModel());

        car.getFeatures().clear();
        carRepository.delete(car);

        searchCache.clear();
        log.info("🗑️ Машина удалена, кэш очищен");
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByBrand(String brand) {
        return carRepository.findByBrandIgnoreCase(brand);
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByFeatureCategoryJpql(String category) {
        log.info(" JPQL: Поиск автомобилей с категорией особенностей: {}", category);
        List<Car> cars = carRepository.findCarsByFeatureCategoryJpql(category);
        log.info(" JPQL: Найдено {} автомобилей", cars.size());
        return cars;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CarResponseDto> findCarsWithPaginationJpql(CarSearchRequest request) {

        CarCacheKey cacheKey = new CarCacheKey(
                request.getFeatureCategory(),
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortDirection()
        );

        PageResponseDto<CarResponseDto> cachedResult = searchCache.get(cacheKey);
        if (cachedResult != null) {
            log.info(" ОТВЕТ ИЗ КЭША для {}", cacheKey);
            return cachedResult;
        }

        log.info(" Ищем в БД для {}", cacheKey);

        long startTime = System.currentTimeMillis();

        Sort sort = Sort.by(request.getSortDirection(), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Car> carPage = carRepository.findCarsByFeatureCategoryWithPagination(
                request.getFeatureCategory(),
                pageable
        );

        long dbTime = System.currentTimeMillis() - startTime;
        log.info(" БД вернула результат за {} мс", dbTime);
        log.info(" JPQL: Найдено {} машин на странице, всего {} машин",
                carPage.getNumberOfElements(),
                carPage.getTotalElements());

        PageResponseDto<CarResponseDto> response = mapToPageResponse(carPage);
        searchCache.put(cacheKey, response);

        return response;
    }

    private PageResponseDto<CarResponseDto> mapToPageResponse(Page<Car> carPage) {
        List<CarResponseDto> content = carPage.getContent().stream()
                .map(carMapper::toResponseDto)
                .toList();

        return PageResponseDto.<CarResponseDto>builder()
                .content(content)
                .page(carPage.getNumber())
                .size(carPage.getSize())
                .totalElements(carPage.getTotalElements())
                .totalPages(carPage.getTotalPages())
                .first(carPage.isFirst())
                .last(carPage.isLast())
                .build();
    }
}