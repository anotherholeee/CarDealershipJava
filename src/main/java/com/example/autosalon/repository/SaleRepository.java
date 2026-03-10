package com.example.autosalon.repository;

import com.example.autosalon.entity.Sale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findByCarId(Long carId);

    List<Sale> findByCustomerId(Long customerId);

    @Query("SELECT s FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    List<Sale> findBySaleDateBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}