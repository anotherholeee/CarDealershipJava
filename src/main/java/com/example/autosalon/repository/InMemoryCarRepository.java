package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryCarRepository implements CarRepository {


    private final Map<Long, Car> storage = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Car> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Optional<Car> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Car save(Car car) {
        if (car.getId() == null) {
            car.setId(idGenerator.getAndIncrement());
        }
        storage.put(car.getId(), car);
        return car;
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }

    @Override
    public List<Car> findByBrand(String brand) {
        return storage.values().stream()
                .filter(car -> car.getBrand().equalsIgnoreCase(brand))
                .toList();
    }
}