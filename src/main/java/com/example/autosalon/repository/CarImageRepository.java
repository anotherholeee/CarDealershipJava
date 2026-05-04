package com.example.autosalon.repository;

import com.example.autosalon.entity.CarImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarImageRepository extends JpaRepository<CarImage, Long> {

    List<CarImage> findByCarIdOrderBySortOrderAsc(Long carId);

    int countByCarId(Long carId);

    Optional<CarImage> findByIdAndCarId(Long id, Long carId);

    void deleteByCarId(Long carId);
}
