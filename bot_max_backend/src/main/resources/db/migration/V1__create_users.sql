CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    max_user_id BIGINT NOT NULL UNIQUE,
    language VARCHAR(5),
    citizenship VARCHAR(5),
    track VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_max_user_id ON users(max_user_id);