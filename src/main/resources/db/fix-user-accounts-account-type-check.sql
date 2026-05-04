-- Выполнить один раз в PostgreSQL, если при старте приложения ошибка:
-- user_accounts_account_type_check / значение ADMIN не допускается.

ALTER TABLE user_accounts DROP CONSTRAINT IF EXISTS user_accounts_account_type_check;

ALTER TABLE user_accounts ADD CONSTRAINT user_accounts_account_type_check
    CHECK (account_type IN ('PERSON', 'DEALERSHIP', 'ADMIN'));
