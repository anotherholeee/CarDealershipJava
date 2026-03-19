# 3 ЛИСТИНГ КОДА

Файл CarController.java

```java
/**
 * JPQL версия без пагинации
 * GET /api/cars/search/jpql?category=Комфорт
 */
@GetMapping("/search/jpql")
public ResponseEntity<List<CarResponseDto>> getCarsByFeatureCategoryJpql(
        @RequestParam String category) {
    List<Car> cars = carService.getCarsByFeatureCategoryJpql(category);
    List<CarResponseDto> responseDtos = cars.stream()
            .map(carMapper::toResponseDto)
            .sorted(Comparator.comparing(CarResponseDto::getId))
            .toList();
    return ResponseEntity.ok(responseDtos);
}

/**
 * Native версия без пагинации
 * GET /api/cars/search/native?category=Комфорт
 */
@GetMapping("/search/native")
public ResponseEntity<List<CarResponseDto>> searchCarsByFeatureCategoryNative(
        @RequestParam String category) {
    List<Car> cars = carService.getCarsByFeatureCategoryNative(category);
    List<CarResponseDto> responseDtos = cars.stream()
            .map(carMapper::toResponseDto)
            .sorted(Comparator.comparing(CarResponseDto::getId))
            .toList();
    return ResponseEntity.ok(responseDtos);
}

/**
 * JPQL с пагинацией и кэшированием
 * GET /api/cars/pagination/jpql?featureCategory=Комфорт&page=0&size=5
 */
@GetMapping("/pagination/jpql")
public ResponseEntity<PageResponseDto<CarResponseDto>> getCarsWithPaginationJpql(
        @ModelAttribute CarSearchRequest request) {
    PageResponseDto<CarResponseDto> response = carService.findCarsWithPaginationJpql(request);
    return ResponseEntity.ok(response);
}

/**
 * Native Query с пагинацией и кэшированием
 * GET /api/cars/pagination/native?featureCategory=Безопасность&page=1&size=3&sortBy=price&sortDirection=DESC
 */
@GetMapping("/pagination/native")
public ResponseEntity<PageResponseDto<CarResponseDto>> getCarsWithPaginationNative(
        @ModelAttribute CarSearchRequest request) {
    PageResponseDto<CarResponseDto> response = carService.findCarsWithPaginationNative(request);
    return ResponseEntity.ok(response);
}
```

Файл CarRepository.java

```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByBrandIgnoreCase(String brand);

    @Override
    @EntityGraph(attributePaths = {"features", "sale"})
    List<Car> findAll();

    /**
     * JPQL запрос для поиска по категории особенностей (вложенная сущность Feature)
     */
    @Query("SELECT DISTINCT c FROM Car c JOIN c.features f WHERE f.category = :category")
    List<Car> findCarsByFeatureCategoryJpql(@Param("category") String category);

    /**
     * Native Query для поиска по категории особенностей
     */
    @Query(value = "SELECT DISTINCT c.* FROM cars c "
            + "INNER JOIN car_features cf ON c.id = cf.car_id "
            + "INNER JOIN features f ON cf.feature_id = f.id "
            + "WHERE f.category = :category",
            nativeQuery = true)
    List<Car> findCarsByFeatureCategoryNative(@Param("category") String category);

    /**
     * JPQL с пагинацией
     */
    @Query("SELECT DISTINCT c FROM Car c "
            + "LEFT JOIN c.features f "
            + "WHERE (:category IS NULL OR f.category = :category)")
    Page<Car> findCarsByFeatureCategoryWithPagination(
            @Param("category") String category,
            Pageable pageable);

    /**
     * Native Query с пагинацией
     */
    @Query(value = "SELECT DISTINCT c.* FROM cars c "
            + "LEFT JOIN car_features cf ON c.id = cf.car_id "
            + "LEFT JOIN features f ON cf.feature_id = f.id "
            + "WHERE (:category IS NULL OR f.category = :category)",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM cars c "
                    + "LEFT JOIN car_features cf ON c.id = cf.car_id "
                    + "LEFT JOIN features f ON cf.feature_id = f.id "
                    + "WHERE (:category IS NULL OR f.category = :category)",
            nativeQuery = true)
    Page<Car> findCarsByFeatureCategoryNativeWithPagination(
            @Param("category") String category,
            Pageable pageable);
}
```

Файл CarService.java

```java
private final CarMapper carMapper;
private final CarSearchCache searchCache;

@Transactional
public Car createCar(Car car) {
    car.setId(null);
    Car saved = carRepository.save(car);
    searchCache.clear();
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
    return existingCar;
}

@Transactional
public void deleteCar(Long id) {
    Car car = self.getObject().getCarById(id);
    if (car.getSale() != null) {
        throw new IllegalStateException("Невозможно удалить машину - она уже продана!");
    }
    car.getFeatures().clear();
    carRepository.delete(car);
    searchCache.clear();
}

@Transactional(readOnly = true)
public List<Car> getCarsByFeatureCategoryJpql(String category) {
    return carRepository.findCarsByFeatureCategoryJpql(category);
}

@Transactional(readOnly = true)
public List<Car> getCarsByFeatureCategoryNative(String category) {
    return carRepository.findCarsByFeatureCategoryNative(category);
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
    if (cachedResult != null) return cachedResult;

    Sort sort = Sort.by(request.getSortDirection(), request.getSortBy());
    Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
    Page<Car> carPage = carRepository.findCarsByFeatureCategoryWithPagination(
            request.getFeatureCategory(), pageable);
    PageResponseDto<CarResponseDto> response = mapToPageResponse(carPage);
    searchCache.put(cacheKey, response);
    return response;
}

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
    if (cachedResult != null) return cachedResult;

    Sort sort = Sort.by(request.getSortDirection(), request.getSortBy());
    Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
    Page<Car> carPage = carRepository.findCarsByFeatureCategoryNativeWithPagination(
            request.getFeatureCategory(), pageable);
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
```

Файл CarCacheKey.java

```java
package com.example.autosalon.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.domain.Sort;

/**
 * Составной ключ для кэша
 * equals() и hashCode() генерируются Lombok (@EqualsAndHashCode)
 */
@Getter
@EqualsAndHashCode
public class CarCacheKey {
    private final String category;
    private final int page;
    private final int size;
    private final String sortBy;
    private final Sort.Direction direction;

    public CarCacheKey(
            String category, int page, int size, String sortBy, Sort.Direction direction) {
        this.category = category;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.direction = direction;
    }

    @Override
    public String toString() {
        return String.format("CarCacheKey{category=%s, page=%d, size=%d, sortBy=%s, direction=%s}",
                category, page, size, sortBy, direction);
    }
}
```

Файл CarSearchCache.java

```java
package com.example.autosalon.cache;

import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.PageResponseDto;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CarSearchCache {

    private final ConcurrentHashMap<CarCacheKey, PageResponseDto<CarResponseDto>> cache =
            new ConcurrentHashMap<>();

    public PageResponseDto<CarResponseDto> get(CarCacheKey key) {
        PageResponseDto<CarResponseDto> result = cache.get(key);
        if (result != null) {
            log.info("✅ КЭШ HIT: ключ={}", key);
        } else {
            log.info("❌ КЭШ MISS: ключ={}", key);
        }
        return result;
    }

    public void put(CarCacheKey key, PageResponseDto<CarResponseDto> value) {
        cache.put(key, value);
        log.info("💾 КЭШ СОХРАНЕН: ключ={}, размер кэша={}", key, cache.size());
    }

    public void clear() {
        cache.clear();
        log.info("🧹 КЭШ ОЧИЩЕН");
    }

    public void remove(CarCacheKey key) {
        cache.remove(key);
        log.info("🗑️ КЭШ УДАЛЕН: ключ={}", key);
    }

    public int size() {
        return cache.size();
    }
}
```

Файл CarSearchRequest.java

```java
package com.example.autosalon.dto;

import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class CarSearchRequest {

    private String brand;
    private String model;
    private Integer yearFrom;
    private Integer yearTo;
    private Double priceFrom;
    private Double priceTo;
    private String color;
    private String featureCategory;

    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private Sort.Direction sortDirection = Sort.Direction.ASC;
}
```

Файл PageResponseDto.java

```java
package com.example.autosalon.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
```

Файл CacheController.java

```java
package com.example.autosalon.controller;

import com.example.autosalon.cache.CarSearchCache;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CarSearchCache carSearchCache;

    /**
     * Получить информацию о кэше
     * GET /api/cache/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("size", carSearchCache.size());
        info.put("message", "Кэш хранит результаты поиска машин с пагинацией");
        return ResponseEntity.ok(info);
    }

    /**
     * Очистить кэш
     * GET /api/cache/clear
     */
    @GetMapping("/clear")
    public ResponseEntity<String> clearCache() {
        carSearchCache.clear();
        return ResponseEntity.ok("Кэш успешно очищен");
    }
}
```
