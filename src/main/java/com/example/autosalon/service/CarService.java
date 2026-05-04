package com.example.autosalon.service;

import com.example.autosalon.cache.CarCacheKey;
import com.example.autosalon.cache.CarSearchCache;
import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.CarSearchRequest;
import com.example.autosalon.dto.PageResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.enums.AccountType;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.SaleRepository;
import com.example.autosalon.repository.UserAccountRepository;
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
    private final UserAccountRepository userAccountRepository;
    private final CarMapper carMapper;
    private final CarSearchCache searchCache;
    private final ObjectProvider<CarService> self;
    private final CarImageService carImageService;

    @Transactional(readOnly = true)
    public List<Car> getAllCars() {
        return carRepository.findAllWithAllRelations();
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByOwner(UserAccount owner) {
        return carRepository.findByOwnerId(owner.getId());
    }

    /**
     * Маппинг в DTO внутри той же read-only транзакции, чтобы коллекции features/images
     * гарантированно были инициализированы (избегаем пустого photos в /cars/mine).
     */
    @Transactional(readOnly = true)
    public List<CarResponseDto> getCarsByOwnerAsDtos(UserAccount owner) {
        List<Car> cars = carRepository.findByOwnerId(owner.getId());
        List<CarResponseDto> dtos = new ArrayList<>(cars.size());
        for (Car car : cars) {
            if (car.getImages() != null) {
                car.getImages().size();
            }
            if (car.getFeatures() != null) {
                car.getFeatures().size();
            }
            dtos.add(carMapper.toResponseDto(car));
        }
        dtos.sort(Comparator.comparing(CarResponseDto::getId));
        return dtos;
    }

    @Transactional(readOnly = true)
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + id + " не найдена"));

    }

    @Transactional
    public Car createCar(Car car, UserAccount actor, Long ownerUserId) {
        UserAccount owner = resolveOwner(actor, ownerUserId);
        car.setId(null);
        car.setOwner(owner);
        Car saved = carRepository.save(car);
        searchCache.clear();
        log.info(" Создана новая машина, кэш очищен");
        return saved;
    }

    private UserAccount resolveOwner(UserAccount actor, Long ownerUserId) {
        if (ownerUserId == null) {
            return actor;
        }
        if (actor.getAccountType() != AccountType.ADMIN) {
            throw new IllegalStateException("Указывать владельца может только администратор");
        }
        return userAccountRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Пользователь-владелец не найден: id=" + ownerUserId));
    }

    private boolean canManageCar(Car car, UserAccount actor) {
        if (AuthService.isAdmin(actor)) {
            return true;
        }
        return car.getOwner() != null && car.getOwner().getId().equals(actor.getId());
    }

    /**
     * Пакетное создание объявлений для автосалона: все позиции пакета привязываются к владельцу.
     * Дубликаты внутри одного запроса (марка+модель+год без учёта регистра) не допускаются.
     */
    @Transactional
    public List<CarResponseDto> createDealershipCarBulk(UserAccount owner, List<CarRequestDto> requests) {
        Objects.requireNonNull(owner, "owner");
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Список автомобилей не может быть пустым");
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < requests.size(); i++) {
            CarRequestDto dto = requests.get(i);
            String key = dto.getBrand().trim().toLowerCase(Locale.ROOT) + "|"
                    + dto.getModel().trim().toLowerCase(Locale.ROOT) + "|" + dto.getYear();
            if (!seen.add(key)) {
                throw new IllegalArgumentException(String.format(
                        "В пакете повторяется объявление: %s %s %d (позиция %d)",
                        dto.getBrand().trim(),
                        dto.getModel().trim(),
                        dto.getYear(),
                        i + 1));
            }
        }
        List<Car> toSave = new ArrayList<>(requests.size());
        for (CarRequestDto dto : requests) {
            Car car = carMapper.toEntity(dto);
            car.setId(null);
            car.setOwner(resolveOwner(owner, dto.getOwnerUserId()));
            toSave.add(car);
        }
        List<Car> saved = carRepository.saveAll(toSave);
        searchCache.clear();
        log.info("Пакет автосалона: сохранено {} объявлений для пользователя id={}", saved.size(), owner.getId());
        return saved.stream().map(carMapper::toResponseDto).toList();
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
    public Car updateCar(Long id, Car carDetails, UserAccount actor) {
        Car existingCar = self.getObject().getCarById(id);
        if (!canManageCar(existingCar, actor)) {
            throw new IllegalStateException("Можно изменять только свои объявления");
        }

        existingCar.setBrand(carDetails.getBrand());
        existingCar.setModel(carDetails.getModel());
        existingCar.setYear(carDetails.getYear());
        existingCar.setColor(carDetails.getColor());
        existingCar.setInteriorColor(carDetails.getInteriorColor());
        existingCar.setInteriorMaterial(carDetails.getInteriorMaterial());
        existingCar.setEngineVolume(carDetails.getEngineVolume());
        existingCar.setMileage(carDetails.getMileage());
        existingCar.setPowerHp(carDetails.getPowerHp());
        existingCar.setFuelConsumptionCity(carDetails.getFuelConsumptionCity());
        existingCar.setFuelConsumptionHighway(carDetails.getFuelConsumptionHighway());
        existingCar.setFuelConsumptionMixed(carDetails.getFuelConsumptionMixed());
        existingCar.setSeatCount(carDetails.getSeatCount());
        existingCar.setCity(carDetails.getCity());
        existingCar.setTransmission(carDetails.getTransmission());
        existingCar.setBodyType(carDetails.getBodyType());
        existingCar.setEngineType(carDetails.getEngineType());
        existingCar.setDriveType(carDetails.getDriveType());
        existingCar.setPrice(carDetails.getPrice());
        String cur = carDetails.getPriceCurrency();
        existingCar.setPriceCurrency(
                cur != null && !cur.isBlank() ? cur.trim().toUpperCase(Locale.ROOT) : "USD");

        List<Feature> toDetach = new ArrayList<>(existingCar.getFeatures());
        for (Feature f : toDetach) {
            existingCar.removeFeature(f);
        }
        if (carDetails.getFeatures() != null) {
            for (Feature f : carDetails.getFeatures()) {
                existingCar.addFeature(f);
            }
        }

        searchCache.clear();
        log.info(" Машина обновлена, кэш очищен");

        return existingCar;
    }

    @Transactional
    public void deleteCar(Long id, UserAccount actor) {
        Car car = self.getObject().getCarById(id);
        if (actor != null && !canManageCar(car, actor)) {
            throw new IllegalStateException("Можно удалять только свои объявления");
        }

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

        carImageService.deleteAllFilesForCar(car.getId());
        car.getFeatures().clear();
        carRepository.delete(car);

        searchCache.clear();
        log.info("🗑️ Машина удалена, кэш очищен");
    }

    @Transactional
    public void deleteCar(Long id) {
        deleteCar(id, null);
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