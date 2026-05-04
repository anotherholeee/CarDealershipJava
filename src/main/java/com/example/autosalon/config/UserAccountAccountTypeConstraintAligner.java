package com.example.autosalon.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * В БД могло остаться CHECK только для PERSON/DEALERSHIP (до появления {@code ADMIN} в enum).
 * Тогда вставка админа в {@link com.example.autosalon.DataInitializer} падает с 23514.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UserAccountAccountTypeConstraintAligner implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgreSQL()) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "alter table user_accounts drop constraint if exists user_accounts_account_type_check");
            jdbcTemplate.execute(
                    "alter table user_accounts add constraint user_accounts_account_type_check "
                            + "check (account_type in ('PERSON','DEALERSHIP','ADMIN'))");
            log.info("Схема: ограничение user_accounts.account_type обновлено (добавлен ADMIN).");
        } catch (DataAccessException ex) {
            log.warn(
                    "Не удалось обновить CHECK для user_accounts.account_type. "
                            + "Выполните вручную скрипт db/fix-user-accounts-account-type-check.sql. Причина: {}",
                    ex.getMessage());
        }
    }

    private boolean isPostgreSQL() {
        try (Connection c = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(c.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            log.debug("Не удалось определить СУБД: {}", e.getMessage());
            return false;
        }
    }
}
