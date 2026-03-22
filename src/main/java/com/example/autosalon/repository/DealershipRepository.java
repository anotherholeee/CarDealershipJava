package com.example.autosalon.repository;

import com.example.autosalon.entity.Dealership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DealershipRepository extends JpaRepository<Dealership, Long> {

    List<Dealership> findByNameContainingIgnoreCase(String name);

    @Query("SELECT d FROM Dealership d WHERE SIZE(d.cars) > :minCars")
    List<Dealership> findDealershipsWithMinCars(@Param("minCars") int minCars);

    @EntityGraph(attributePaths = {"cars"})
    @Query("SELECT DISTINCT d FROM Dealership d")
    List<Dealership> findAllWithCars();

    @EntityGraph(attributePaths = {"cars", "cars.sale"})
    @Query("SELECT d FROM Dealership d WHERE d.id = :id")
    Optional<Dealership> findByIdWithCars(@Param("id") Long id);
}