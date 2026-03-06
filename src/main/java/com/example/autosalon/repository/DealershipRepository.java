package com.example.autosalon.repository;

import com.example.autosalon.entity.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью Dealership (автосалоны)
 * Предоставляет базовые CRUD операции:
 * - findAll() - получить все автосалоны
 * - findById(id) - найти по ID
 * - save(dealership) - сохранить/обновить
 * - deleteById(id) - удалить по ID
 * - count() - количество записей
 */
@Repository
public interface DealershipRepository extends JpaRepository<Dealership, Long> {
}