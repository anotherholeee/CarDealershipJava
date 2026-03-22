package com.example.autosalon.repository;

import com.example.autosalon.entity.Customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    @EntityGraph(attributePaths = {"sales"})
    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.sales WHERE c.id = :id")
    Optional<Customer> findByIdWithSales(@Param("id") Long id);

    @EntityGraph(attributePaths = {"sales"})
    @Query("SELECT c FROM Customer c")
    List<Customer> findAllWithSales();
}