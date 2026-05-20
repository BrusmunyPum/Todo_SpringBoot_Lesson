ALTER TABLE app_users
    ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT 'temporary_password';