package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByBrandIgnoreCase(String brand);

    @Override
    @EntityGraph(attributePaths = {"features", "sale"})
    List<Car> findAll();

    /**
     * JPQL запрос для поиска по категории особенностей
     */
    @Query("SELECT DISTINCT c FROM Car c JOIN c.features f WHERE f.category = :category")
    List<Car> findCarsByFeatureCategoryJpql(@Param("category") String category);

    /**
     * Native Query для поиска по категории особенностей
     */
    @Query(value = "SELECT DISTINCT c.* FROM cars c "
            + "INNER JOIN car_features cf ON c.id = cf.car_id "
            + "INNER JOIN features f ON cf.feature_id = f.id "
            + "WHERE f.category = :category",
            nativeQuery = true)
    List<Car> findCarsByFeatureCategoryNative(@Param("category") String category);
    /**
     * НОВЫЙ МЕТОД 1: JPQL с пагинацией
     * LEFT JOIN и условие IS NULL позволяют не фильтровать, если категория не указана
     */

    @Query("SELECT DISTINCT c FROM Car c "
            + "LEFT JOIN c.features f "
            + "WHERE (:category IS NULL OR f.category = :category)")
    Page<Car> findCarsByFeatureCategoryWithPagination(
            @Param("category") String category,
            Pageable pageable);

    /**
     * НОВЫЙ МЕТОД 2: Native Query с пагинацией
     * Нужен отдельный countQuery для подсчета общего количества
     */
    @Query(value = "SELECT DISTINCT c.* FROM cars c "
            +
            "LEFT JOIN car_features cf ON c.id = cf.car_id "
            +
            "LEFT JOIN features f ON cf.feature_id = f.id "
            +
            "WHERE (:category IS NULL OR f.category = :category)",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM cars c "
                    + "LEFT JOIN car_features cf ON c.id = cf.car_id "
                    + "LEFT JOIN features f ON cf.feature_id = f.id "
                    + "WHERE (:category IS NULL OR f.category = :category)",
            nativeQuery = true)
    Page<Car> findCarsByFeatureCategoryNativeWithPagination(
            @Param("category") String category,
            Pageable pageable);
}
