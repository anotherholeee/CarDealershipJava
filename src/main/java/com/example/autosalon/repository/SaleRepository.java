package com.example.autosalon.repository;

import com.example.autosalon.entity.Sale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"car", "customer"})
    Optional<Sale> findByCarId(Long carId);

    @EntityGraph(attributePaths = {"car", "customer"})
    List<Sale> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"car", "customer"})
    @Query("SELECT s FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    List<Sale> findBySaleDateBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Override
    @EntityGraph(attributePaths = {"car", "customer"})
    List<Sale> findAll();

    @Override
    @EntityGraph(attributePaths = {"car", "customer"})
    Optional<Sale> findById(Long id);
}