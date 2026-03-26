package com.example.autosalon.service;

import com.example.autosalon.cache.CarCacheKey;
import com.example.autosalon.cache.CarSearchCache;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.CarSearchRequest;
import com.example.autosalon.dto.PageResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.SaleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<Car> getCarsWithSolution() {
        log.info("=== РЕШЕНИЕ: findAll с @EntityGraph ===");
        return carRepository.findAllWithAllRelations();
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByFeatureCategoryJpql(String category) {
        log.info(" JPQL: Поиск автомобилей с категорией особенностей: {}", category);
        List<Car> cars = carRepository.findCarsByFeatureCategoryJpql(category);
        log.info(" JPQL: Найдено {} автомобилей", cars.size());
        return cars;
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByFeatureCategoryNative(String category) {
        log.info("🟢 NATIVE: Поиск автомобилей с категорией особенностей: {}", category);
        List<Car> cars = carRepository.findCarsByFeatureCategoryNative(category);
        log.info("🟢 NATIVE: Найдено {} автомобилей", cars.size());
        return cars;
    }

    /**
     * JPQL с пагинацией и кэшированием
     */
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

    /**
     * Native Query с пагинацией и кэшированием
     */
    @Transactional(readOnly = true)
    public PageResponseDto<CarResponseDto> findCarsWithPaginationNative(CarSearchRequest request) {

        CarCacheKey cacheKey = new CarCacheKey(
                request.getFeatureCategory(),
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortDirection()
        );

        PageResponseDto<CarResponseDto> cachedResult = searchCache.get(cacheKey);
        if (cachedResult != null) {
            log.info(" NATIVE: ответ из кэша для {}", cacheKey);
            return cachedResult;
        }

        log.info(" NATIVE: ищем в БД для {}", cacheKey);

        Sort sort = Sort.by(request.getSortDirection(), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Car> carPage = carRepository.findCarsByFeatureCategoryNativeWithPagination(
                request.getFeatureCategory(),
                pageable
        );

        log.info(" NATIVE: Найдено {} машин на странице, всего {} машин",
                carPage.getNumberOfElements(),
                carPage.getTotalElements());

        PageResponseDto<CarResponseDto> response = mapToPageResponse(carPage);
        searchCache.put(cacheKey, response);

        return response;
    }

    /**
     * Вспомогательный метод для преобразования Page<Car> в PageResponseDto<CarResponseDto>
     */
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