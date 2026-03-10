# Листинг нового кода (этап 2)

Ниже приведён **только новый код**, добавленный после 1-й лабораторной:

- **Новые файлы** — показаны целиком.
- **Старые файлы** (которые были в 1-й работе) — показаны **только новыми/добавленными фрагментами**, без полного повторения старого листинга.

---

## Новое в существующих файлах

### `src/main/java/com/example/autosalon/dto/CarRequestDto.java` — добавлено поле `featureIds`

```java
private List<Long> featureIds;
```

---

### `src/main/java/com/example/autosalon/controller/CarController.java` — добавлены endpoint’ы для N+1

```java
/**
 * Демонстрация проблемы N+1
 * GET /api/cars/features/problem
 */
@GetMapping("/features/problem")
public ResponseEntity<List<Car>> demonstrateNplusOneProblem() {
    List<Car> cars = carService.getCarsWithNplusOneProblem();
    return ResponseEntity.ok(cars);
}

/**
 * Демонстрация решения с @EntityGraph
 * GET /api/cars/features/solution
 */
@GetMapping("/features/solution")
public ResponseEntity<List<Car>> demonstrateSolution() {
    List<Car> cars = carService.getCarsWithSolution();
    return ResponseEntity.ok(cars);
}
```

---

### `src/main/java/com/example/autosalon/mapper/CarMapper.java` — добавлено подтягивание `Feature` по `featureIds`

```java
private final FeatureRepository featureRepository;

// ...

if (dto.getFeatureIds() != null && !dto.getFeatureIds().isEmpty()) {
    List<Feature> features = featureRepository.findAllById(dto.getFeatureIds());
    car.setFeatures(features);
}
```

---

### `src/main/java/com/example/autosalon/service/CarService.java` — добавлены методы для N+1 / EntityGraph

```java
private final CarRepositoryWithoutGraph carRepositoryWithout;

// ...

@Transactional(readOnly = true)
public List<Car> getCarsWithNplusOneProblem() {
    log.info("=== ПРОБЛЕМА N+1: обычный findAll ===");
    return carRepositoryWithout.findAll();
}

@Transactional(readOnly = true)
public List<Car> getCarsWithSolution() {
    log.info("=== РЕШЕНИЕ: findAll с @EntityGraph ===");
    return carRepository.findAll();
}
```

---

### `src/main/java/com/example/autosalon/repository/CarRepository.java` — добавлен `@EntityGraph`

```java
@Override
@EntityGraph(attributePaths = {"features", "sale"})
List<Car> findAll();
```

---

## Новые файлы (добавлены на этапе 2)

### `src/main/java/com/example/autosalon/dto/DealershipWithCarsRequest.java`

```java
package com.example.autosalon.dto;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import java.util.List;
import lombok.Data;

@Data
public class DealershipWithCarsRequest {
    private Dealership dealership;
    private List<Car> cars;
}
```

---

### `src/main/java/com/example/autosalon/controller/DealershipController.java`

```java
package com.example.autosalon.controller;

import com.example.autosalon.entity.Dealership;
import com.example.autosalon.service.DealershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dealerships")
@RequiredArgsConstructor
public class DealershipController {

    private final DealershipService dealershipService;

    /**
     * Демонстрация БЕЗ транзакции (частичное сохранение)
     * POST /api/dealerships/without-transaction
     */
    @PostMapping("/without-transaction")
    public String createWithoutTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info("%n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/without-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info("📊 До операции: салонов в БД = {}", beforeCount);

        try {
            Dealership saved = dealershipService
                    .createDealershipWithCarsWithoutTransaction(
                            request.getDealership(),
                            request.getCars()
                    );

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    "✅ УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. "
                            + "Салон '%s' сохранен!",
                    beforeCount, afterCount, saved.getName()
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error("Ошибка при сохранении: {}", e.getMessage());
            return String.format(
                    "❌ ОШИБКА: %s%n📊 Салонов было: %d, стало: %d. "
                            + "Видите? Салон сохранился, хотя должна была быть ошибка! "
                            + "Это частичное сохранение.",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }

    /**
     * Демонстрация С транзакцией (полный откат)
     * POST /api/dealerships/with-transaction
     */
    @PostMapping("/with-transaction")
    public String createWithTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info("%n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/with-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info("📊 До операции: салонов в БД = {}", beforeCount);

        try {
            dealershipService.createDealershipWithCarsWithTransaction(
                    request.getDealership(),
                    request.getCars()
            );

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    "✅ УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d.",
                    beforeCount, afterCount
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error("Ошибка при сохранении, транзакция откатилась: {}", e.getMessage());
            return String.format(
                    "✅ ОТКАТ: %s%n📊 Салонов было: %d, стало: %d. "
                            + "Отлично! Транзакция сработала - салон НЕ сохранился!",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }
}
```

---

### `src/main/java/com/example/autosalon/service/DealershipService.java`

```java
package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealershipService {

    private final DealershipRepository dealershipRepository;
    private final CarRepository carRepository;

    public Dealership createDealershipWithCarsWithoutTransaction(
            Dealership dealership, List<Car> cars) {
        log.info("\n");
        log.info("========== ДЕМОНСТРАЦИЯ БЕЗ @Transactional ==========");
        log.info("Начинаем сохранение автосалона и машин...");

        Dealership savedDealership = dealershipRepository.save(dealership);
        log.info("✅ Шаг 1: Автосалон '{}' сохранен в БД",
                savedDealership.getName());

        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            car.setDealership(savedDealership);

            if (i == 1) {
                log.error("❌ Шаг {}: ОШИБКА при сохранении машины {} {} !!!",
                        i + 2, car.getBrand(), car.getModel());
                throw new IllegalArgumentException(
                        "💥 Ошибка сохранения машины: "
                                + car.getBrand() + " " + car.getModel());
            }

            Car savedCar = carRepository.save(car);
            log.info("✅ Шаг {}: Машина {} {} сохранена",
                    i + 2, savedCar.getBrand(), savedCar.getModel());
        }

        log.info("✅ Все данные успешно сохранены!");
        return savedDealership;
    }

    @Transactional
    public Dealership createDealershipWithCarsWithTransaction(
            Dealership dealership, List<Car> cars) {
        log.info("\n");
        log.info("========== ДЕМОНСТРАЦИЯ С @Transactional ==========");
        log.info("Начинаем сохранение автосалона и машин...");

        Dealership savedDealership = dealershipRepository.save(dealership);
        log.info("✅ Шаг 1: Автосалон '{}' сохранен (в транзакции)",
                savedDealership.getName());

        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            car.setDealership(savedDealership);

            if (i == 1) {
                log.error("❌ Шаг {}: ОШИБКА при сохранении машины {} {} !!!",
                        i + 2, car.getBrand(), car.getModel());
                log.info("🔄 Транзакция откатывается... "
                        + "Все изменения будут отменены!");
                throw new IllegalArgumentException(
                        "💥 Ошибка сохранения машины: "
                                + car.getBrand() + " " + car.getModel());
            }

            Car savedCar = carRepository.save(car);
            log.info("✅ Шаг {}: Машина {} {} сохранена",
                    i + 2, savedCar.getBrand(), savedCar.getModel());
        }

        log.info("✅ Все данные успешно сохранены!");
        return savedDealership;
    }

    public long countDealerships() {
        return dealershipRepository.count();
    }
}
```

---

### `src/main/java/com/example/autosalon/entity/Dealership.java`

```java
package com.example.autosalon.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "dealerships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cars")
public class Dealership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @OneToMany(mappedBy = "dealership", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Car> cars = new ArrayList<>();

    public void addCar(Car car) {
        cars.add(car);
        car.setDealership(this);
    }

    public void removeCar(Car car) {
        cars.remove(car);
        car.setDealership(null);
    }
}
```

---

### `src/main/java/com/example/autosalon/entity/Customer.java`

```java
package com.example.autosalon.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "sales")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sale> sales = new ArrayList<>();

    public void addSale(Sale sale) {
        sales.add(sale);
        sale.setCustomer(this);
    }

    public void removeSale(Sale sale) {
        sales.remove(sale);
        sale.setCustomer(null);
    }
}
```

---

### `src/main/java/com/example/autosalon/entity/Sale.java`

```java
package com.example.autosalon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", unique = true)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "price")
    private double salePrice;
}
```

---

### `src/main/java/com/example/autosalon/entity/Feature.java`

```java
package com.example.autosalon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "features")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @ManyToMany(mappedBy = "features")
    @JsonIgnore
    private List<Car> cars = new ArrayList<>();
}
```

---

### `src/main/java/com/example/autosalon/repository/DealershipRepository.java`

```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DealershipRepository extends JpaRepository<Dealership, Long> {
}
```

---

### `src/main/java/com/example/autosalon/repository/FeatureRepository.java`

```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {
}
```

---

### `src/main/java/com/example/autosalon/repository/CustomerRepository.java`

```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
}
```

---

### `src/main/java/com/example/autosalon/repository/SaleRepository.java`

```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Sale;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findByCarId(Long carId);
    List<Sale> findByCustomerId(Long customerId);
}
```

