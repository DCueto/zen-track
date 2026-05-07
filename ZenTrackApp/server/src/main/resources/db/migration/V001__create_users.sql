CREATE TABLE IF NOT EXISTS users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    name          TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_email_key UNIQUE (email)
);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- Users may only read/modify their own row.
-- The application sets app.user_id via SET LOCAL at the start of each transaction.
CREATE POLICY users_self
    ON users
    USING      (id = current_setting('app.user_id', true)::uuid)
    WITH CHECK (id = current_setting('app.user_id', true)::uuid);
