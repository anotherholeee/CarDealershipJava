package com.example.autosalon.repository;

import com.example.autosalon.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью Feature (особенности автомобилей)
 * Предоставляет базовые CRUD операции:
 * - findAll() - получить все особенности
 * - findById(id) - найти по ID
 * - save(feature) - сохранить/обновить
 * - deleteById(id) - удалить по ID
 * - count() - количество записей
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {
}