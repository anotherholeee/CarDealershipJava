package com.example.autosalon.repository;

import com.example.autosalon.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью Sale (продажи)
 * Предоставляет базовые CRUD операции:
 * - findAll() - получить все продажи
 * - findById(id) - найти по ID
 * - save(sale) - сохранить/обновить
 * - deleteById(id) - удалить по ID
 * - count() - количество записей
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByCarId(Long carId);

    List<Sale> findByCustomerId(Long customerId);


}