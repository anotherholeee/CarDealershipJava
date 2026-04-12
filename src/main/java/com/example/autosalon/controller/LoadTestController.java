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