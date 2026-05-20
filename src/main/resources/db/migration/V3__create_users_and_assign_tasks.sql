CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE tasks
    ADD COLUMN user_id BIGINT;

UPDATE tasks
SET user_id = (
    SELECT id
    FROM app_users
             LIMIT 1
    )
WHERE user_id IS NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id)
            REFERENCES app_users(id);
