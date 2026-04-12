# 6 ЛИСТИНГ КОДА

Файл AsyncConfig.java

```java
package com.example.autosalon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
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

Файл AsyncTaskResponseDto.java

```java
package com.example.autosalon.dto;

import com.example.autosalon.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskResponseDto {
    private long taskId;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCarProcessingService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final Map<Long, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final AtomicLong atomicCounter = new AtomicLong(0);
    private long unsafeCounter = 0;
    private long synchronizedCounter = 0;

    public long createNewTask() {
        long taskId = idGenerator.getAndIncrement();
        taskStatusMap.put(taskId, TaskStatus.ACCEPTED);
        return taskId;
    }

    @Async("taskExecutor")
    public void processCarsAsync(Long taskId, List<CarRequestDto> carsDto) {
        try {
            taskStatusMap.put(taskId, TaskStatus.IN_PROGRESS);
            List<Car> cars = carsDto.stream().map(carMapper::toEntity).toList();
            carRepository.saveAll(cars);
            taskStatusMap.put(taskId, TaskStatus.SAVED);
            Thread.sleep(2000);
            taskStatusMap.put(taskId, TaskStatus.COMPLETED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            taskStatusMap.put(taskId, TaskStatus.FAILED);
        }
    }

    public TaskStatus getTaskStatus(Long id) {
        return taskStatusMap.get(id);
    }

    public Map<String, Object> runRaceConditionTest(int threads, int operations) throws InterruptedException {
        unsafeCounter = 0;
        ExecutorService testExecutor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < operations; i++) {
            testExecutor.submit(() -> unsafeCounter++);
        }
        testExecutor.shutdown();
        testExecutor.awaitTermination(10, TimeUnit.SECONDS);
        return Map.of(
                "counterType", "UNSAFE",
                "expectedValue", operations,
                "actualValue", unsafeCounter
        );
    }

    public Map<String, Object> runAtomicSolutionTest(int threads, int operations) throws InterruptedException {
        atomicCounter.set(0);
        ExecutorService testExecutor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < operations; i++) {
            testExecutor.submit(atomicCounter::incrementAndGet);
        }
        testExecutor.shutdown();
        testExecutor.awaitTermination(10, TimeUnit.SECONDS);
        return Map.of(
                "counterType", "ATOMIC",
                "expectedValue", operations,
                "actualValue", atomicCounter.get()
        );
    }

    public Map<String, Object> runSynchronizedSolutionTest(int threads, int operations) throws InterruptedException {
        synchronizedCounter = 0;
        ExecutorService testExecutor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < operations; i++) {
            testExecutor.submit(this::incrementSynchronizedCounter);
        }
        testExecutor.shutdown();
        testExecutor.awaitTermination(10, TimeUnit.SECONDS);
        return Map.of(
                "counterType", "SYNCHRONIZED",
                "expectedValue", operations,
                "actualValue", synchronizedCounter
        );
    }

    private synchronized void incrementSynchronizedCounter() {
        synchronizedCounter++;
    }
}
```

Файл AsyncController.java

```java
package com.example.autosalon.controller;

import com.example.autosalon.dto.CarListRequestDto;
import com.example.autosalon.dto.AsyncTaskResponseDto;
import com.example.autosalon.enums.TaskStatus;
import com.example.autosalon.service.AsyncCarProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Tag(name = "Async Operations", description = "Асинхронные операции с автомобилями")
public class AsyncController {

    private final AsyncCarProcessingService asyncService;

    @PostMapping("/cars/batch")
    @Operation(summary = "Запустить асинхронную пакетную обработку автомобилей")
    public ResponseEntity<AsyncTaskResponseDto> startAsyncBatch(@Valid @RequestBody CarListRequestDto bulkDto) {
        Long taskId = asyncService.createNewTask();
        asyncService.processCarsAsync(taskId, bulkDto.getCars());

        return ResponseEntity.accepted().body(new AsyncTaskResponseDto(taskId, TaskStatus.ACCEPTED));
    }

    @GetMapping("/status/{taskId}")
    @Operation(summary = "Получить статус асинхронной задачи")
    public ResponseEntity<AsyncTaskResponseDto> getTaskStatus(@PathVariable Long taskId) {
        TaskStatus status = asyncService.getTaskStatus(taskId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new AsyncTaskResponseDto(taskId, status));
    }

    @GetMapping("/test_problem")
    @Operation(summary = "Продемонстрировать проблему Race Condition")
    public Map<String, Object> testRace(
            @RequestParam(defaultValue = "60") int threads,
            @RequestParam(defaultValue = "10000") int operations) throws InterruptedException {
        return asyncService.runRaceConditionTest(threads, operations);
    }

    @GetMapping("/test_solution")
    @Operation(summary = "Продемонстрировать решение Race Condition через AtomicLong")
    public Map<String, Object> testAtomic(
            @RequestParam(defaultValue = "60") int threads,
            @RequestParam(defaultValue = "10000") int operations) throws InterruptedException {
        return asyncService.runAtomicSolutionTest(threads, operations);
    }

    @GetMapping("/test_solution_synchronized")
    @Operation(summary = "Продемонстрировать решение Race Condition через synchronized")
    public Map<String, Object> testSynchronized(
            @RequestParam(defaultValue = "60") int threads,
            @RequestParam(defaultValue = "10000") int operations) throws InterruptedException {
        return asyncService.runSynchronizedSolutionTest(threads, operations);
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
