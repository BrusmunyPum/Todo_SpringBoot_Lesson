-- Add role column to app_users table
-- All existing users get 'USER' role by default

ALTER TABLE app_users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
