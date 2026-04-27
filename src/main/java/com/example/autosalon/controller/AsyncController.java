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

    @Operation(summary = "Продемонстрировать решение Race Condition через synchronized", description = "Корректный счётчик с синхронизированным инкрементом")
    @GetMapping("/test_solution_synchronized")
    public String testSynchronized() throws InterruptedException {
        return asyncCarProcessingService.runSynchronizedSolutionTest();
    }
}
