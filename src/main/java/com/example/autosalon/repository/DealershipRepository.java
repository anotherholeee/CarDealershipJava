package com.example.autosalon.repository;

import com.example.autosalon.entity.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с сущностью Dealership (автосалоны)
 */
@Repository
public interface DealershipRepository extends JpaRepository<Dealership, Long> {

    /**
     * Поиск автосалонов по названию (без учета регистра)
     */
    List<Dealership> findByNameContainingIgnoreCase(String name);

    /**
     * Поиск автосалонов с количеством машин больше указанного
     */
    @Query("SELECT d FROM Dealership d WHERE SIZE(d.cars) > :minCars")
    List<Dealership> findDealershipsWithMinCars(@Param("minCars") int minCars);

    /**
     * Получение автосалонов с их машинами (решение проблемы N+1)
     */
    @Query("SELECT DISTINCT d FROM Dealership d LEFT JOIN FETCH d.cars")
    List<Dealership> findAllWithCars();
}