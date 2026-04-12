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
