package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    @EntityGraph(attributePaths = {"features"})
    List<Car> findByBrandIgnoreCase(String brand);

    @Override
    @EntityGraph(attributePaths = {"features", "sale", "dealership"})
    List<Car> findAll();

    @Override
    @EntityGraph(attributePaths = {"features", "sale", "dealership"})
    Optional<Car> findById(Long id);

    @EntityGraph(attributePaths = {"features", "sale", "dealership"})
    @Query("SELECT DISTINCT c FROM Car c JOIN c.features f WHERE f.category = :category")
    List<Car> findCarsByFeatureCategoryJpql(@Param("category") String category);

    @EntityGraph(attributePaths = {"features", "sale", "dealership"})
    @Query("SELECT DISTINCT c FROM Car c LEFT JOIN c.features f "
            + "WHERE (:category IS NULL OR f.category = :category)")
    Page<Car> findCarsByFeatureCategoryWithPagination(
            @Param("category") String category,
            Pageable pageable);

    @EntityGraph(attributePaths = {"features"})
    @Query("SELECT c FROM Car c")
    List<Car> findAllWithFeaturesOnly();

    @EntityGraph(attributePaths = {"features", "sale"})
    @Query("SELECT c FROM Car c")
    List<Car> findAllWithFeaturesAndSale();

    @EntityGraph(attributePaths = {"features", "sale", "sale.customer", "dealership"})
    @Query("SELECT c FROM Car c")
    List<Car> findAllWithAllRelations();

    @EntityGraph(attributePaths = {"sale", "features"})
    List<Car> findByDealershipId(Long dealershipId);

    @EntityGraph(attributePaths = {"features", "sale", "sale.customer"})
    @Query("SELECT c FROM Car c WHERE c.dealership.id = :dealershipId")
    List<Car> findByDealershipIdWithAllRelations(@Param("dealershipId") Long dealershipId);

    @EntityGraph(attributePaths = {"features"})
    @Query("SELECT c FROM Car c JOIN c.features f WHERE f.id = :featureId")
    List<Car> findCarsByFeatureId(@Param("featureId") Long featureId);
}