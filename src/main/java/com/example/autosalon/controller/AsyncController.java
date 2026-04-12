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