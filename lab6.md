# 6 ЛИСТИНГ КОДА

Файл AsyncConfig.java

```java
package com.example.autosalon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
}
```

Файл TaskStatus.java

```java
package com.example.autosalon.enums;

public enum TaskStatus {
    ACCEPTED,
    IN_PROGRESS,
    SAVED,
    COMPLETED,
    FAILED
}
```

Файл AsyncTaskResponse.java

```java
package com.example.autosalon.dto;

import com.example.autosalon.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AsyncTaskResponse {
    private long id;
    private TaskStatus status;
}
```

Файл AsyncCarProcessingService.java

```java
package com.example.autosalon.service;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.enums.TaskStatus;
import com.example.autosalon.entity.Car;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class AsyncCarProcessingService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final Map<Long, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    private final AtomicLong testAtomicCounter = new AtomicLong(0);
    private long unsafeCounter = 0;

    public long createNewTask() {
        long taskId = idGenerator.getAndIncrement();
        taskStatusMap.put(taskId, TaskStatus.ACCEPTED);
        return taskId;
    }

    @Async
    public void processCarsAsync(Long taskId, List<CarRequestDto> carsDto) {
        try {
            taskStatusMap.put(taskId, TaskStatus.IN_PROGRESS);
            List<Car> cars = carsDto.stream().map(carMapper::toEntity).toList();
            carRepository.saveAll(cars);
            taskStatusMap.put(taskId, TaskStatus.SAVED);
            Thread.sleep(3000);
            taskStatusMap.put(taskId, TaskStatus.COMPLETED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            taskStatusMap.put(taskId, TaskStatus.FAILED);
        }
    }

    public TaskStatus getTaskStatus(Long id) {
        return taskStatusMap.get(id);
    }

    public String runRaceConditionTest() throws InterruptedException {
        unsafeCounter = 0;
        long total = 10000;
        ExecutorService testExecutor = Executors.newFixedThreadPool(60);
        for (int i = 0; i < total; i++) {
            testExecutor.submit(() -> unsafeCounter++);
        }
        testExecutor.shutdown();
        testExecutor.awaitTermination(5, TimeUnit.SECONDS);
        return "Демонстрация проблемы Race Condition:\n"
                + "Ожидание: " + total + "\n"
                + "Получили: " + unsafeCounter + "\n";
    }

    public String runAtomicSolutionTest() throws InterruptedException {
        long total = 10000;
        testAtomicCounter.set(0);
        ExecutorService testExecutor = Executors.newFixedThreadPool(60);
        for (int i = 0; i < total; i++) {
            testExecutor.submit(testAtomicCounter::getAndIncrement);
        }
        testExecutor.shutdown();
        testExecutor.awaitTermination(5, TimeUnit.SECONDS);
        return "Решение проблемы Race Condition:\n"
                + "Ожидание: " + total + "\n"
                + "Получили: " + testAtomicCounter.get() + "\n";
    }
}
```

Файл AsyncController.java

```java
package com.example.autosalon.controller;

import com.example.autosalon.dto.AsyncTaskResponse;
import com.example.autosalon.dto.CarListRequestDto;
import com.example.autosalon.enums.TaskStatus;
import com.example.autosalon.service.AsyncCarProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Асинхронные операции с автомобилями", description = "Пакетная обработка и демонстрация concurrency")
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
public class AsyncController {

    private final AsyncCarProcessingService asyncCarProcessingService;

    @Operation(summary = "Создать автомобили (async bulk)", description = "Создаёт задачу и обрабатывает список в фоне")
    @PostMapping("/cars/batch")
    public ResponseEntity<Map<String, Long>> addAsync(@Valid @RequestBody CarListRequestDto bulkDto) {
        Long id = asyncCarProcessingService.createNewTask();
        asyncCarProcessingService.processCarsAsync(id, bulkDto.getCars());
        return ResponseEntity.accepted().body(Map.of("taskId", id));
    }

    @Operation(summary = "Получить статус задачи", description = "Текущий статус обработки по ID задачи")
    @GetMapping("/status/{id}")
    public AsyncTaskResponse getStatus(@PathVariable Long id) {
        TaskStatus status = asyncCarProcessingService.getTaskStatus(id);
        return new AsyncTaskResponse(id, status);
    }

    @Operation(summary = "Продемонстрировать проблему Race Condition", description = "60 потоков, небезопасный счётчик")
    @GetMapping("/test_problem")
    public String testRace() throws InterruptedException {
        return asyncCarProcessingService.runRaceConditionTest();
    }

    @Operation(summary = "Продемонстрировать решение Race Condition", description = "Корректный счётчик через AtomicLong")
    @GetMapping("/test_solution")
    public String testAtomic() throws InterruptedException {
        return asyncCarProcessingService.runAtomicSolutionTest();
    }
}
```

Файл LoadTestController.java

```java
package com.example.autosalon.controller;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/api/load-test")
@RequiredArgsConstructor
public class LoadTestController {

    private final CarService carService;

    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Map<Long, Long> responseTimes = Collections.synchronizedMap(new HashMap<>());

    @PostMapping("/cars")
    @Operation(summary = "Эндпоинт для нагрузочного тестирования")
    public ResponseEntity<Map<String, Object>> loadTestCreateCar() {
        long startTime = System.nanoTime();
        long requestId = requestCounter.incrementAndGet();

        try {
            CarRequestDto request = new CarRequestDto();
            request.setBrand("LoadTest_" + requestId);
            request.setModel("Model_" + requestId % 100);
            request.setYear(2020 + (int)(requestId % 5));
            request.setColor("TestColor");
            request.setPrice(10000.0 + (requestId % 90000));

            CarResponseDto response = carService.createCarsBulkTransactional(List.of(request)).get(0);

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            responseTimes.put(requestId, durationMs);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("requestId", requestId);
            result.put("durationMs", durationMs);
            result.put("carId", response.getId());

            if (requestId % 100 == 0) {
                log.info("Обработано {} запросов", requestId);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Ошибка при обработке запроса {}: {}", requestId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("requestId", requestId);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Получить статистику нагрузочного тестирования")
    public ResponseEntity<Map<String, Object>> getLoadTestStats() {
        if (responseTimes.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Нет данных"));
        }

        List<Long> times;
        synchronized (responseTimes) {
            times = new ArrayList<>(responseTimes.values());
        }
        Collections.sort(times);

        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = times.get(0);
        long max = times.get(times.size() - 1);
        long p95 = times.get((int)(times.size() * 0.95));
        long p99 = times.get((int)(times.size() * 0.99));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", responseTimes.size());
        stats.put("avgResponseTimeMs", String.format("%.2f", avg));
        stats.put("minResponseTimeMs", min);
        stats.put("maxResponseTimeMs", max);
        stats.put("p95ResponseTimeMs", p95);
        stats.put("p99ResponseTimeMs", p99);

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/reset")
    @Operation(summary = "Сбросить статистику")
    public ResponseEntity<String> resetStats() {
        requestCounter.set(0);
        responseTimes.clear();
        return ResponseEntity.ok("Статистика сброшена");
    }
}
```

## Асинхронная обработка и демонстрация concurrency

- **POST** `/api/async/cars/batch` — создаёт задачу, возвращает **202 Accepted** и JSON `{"taskId": <long>}`; обработка списка машин выполняется в фоне через `@Async`.
- **GET** `/api/async/status/{id}` — текущий `TaskStatus` в теле `AsyncTaskResponse` (если задачи не было, `status` будет `null`).
- **GET** `/api/async/test_problem` — гонка: 60 потоков, 10000 инкрементов `long` без синхронизации (ожидаемо «Получили» меньше 10000).
- **GET** `/api/async/test_solution` — то же с `AtomicLong` (ожидаемо ровно 10000).

Пул для `@Async` задаётся конфигурацией Spring Boot по умолчанию; явный `ThreadPoolTaskExecutor` в проекте не используется.

## Нагрузочное тестирование (JMeter)

Для сценария в JMeter: **Thread Group** → HTTP Request **POST** `http://<host>:<port>/api/load-test/cars` (тело не обязательно, если контроллер без `@RequestBody`). После прогона — **GET** `/api/load-test/stats` для агрегированной статистики (среднее, min/max, p95, p99); перед серией запросов при необходимости **DELETE** `/api/load-test/reset`.

**Результаты прогона** (вставить скриншот Summary Report / Aggregate Report из JMeter и/или JSON ответа `/api/load-test/stats`):

| Параметр | Значение |
|----------|----------|
| Число потоков | |
| Длительность / итераций | |
| Throughput | |
| Среднее время ответа (ms) | |
| Ошибки % | |
| Данные `/api/load-test/stats` | |
