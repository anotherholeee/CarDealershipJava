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