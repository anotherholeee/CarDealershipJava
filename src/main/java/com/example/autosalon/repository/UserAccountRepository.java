package com.example.autosalon.repository;

import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.enums.AccountType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);

    long countByAccountType(AccountType accountType);
}
