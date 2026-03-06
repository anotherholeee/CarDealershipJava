package com.example.autosalon.repository;

import com.example.autosalon.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью Customer (покупатели)
 * Предоставляет базовые CRUD операции:
 * - findAll() - получить всех покупателей
 * - findById(id) - найти по ID
 * - save(customer) - сохранить/обновить
 * - deleteById(id) - удалить по ID
 * - count() - количество записей
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

}