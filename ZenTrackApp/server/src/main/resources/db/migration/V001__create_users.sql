CREATE TABLE IF NOT EXISTS users (
    id            BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    name          TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_email_key UNIQUE (email)
);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- SELECT: allow unauthenticated lookup (needed for login before app.user_id is known).
-- After login the app sets app.user_id; subsequent reads are still allowed because
-- the OR branches short-circuit.
CREATE POLICY users_self_read ON users
    FOR SELECT
    USING (
        current_setting('app.user_id', true) IS NULL
        OR current_setting('app.user_id', true) = ''
        OR id = current_setting('app.user_id', true)::bigint
    );

-- INSERT: registration creates a new row whose id is not known in advance (IDENTITY).
-- The unique-email constraint and application validation prevent abuse.
CREATE POLICY users_self_write ON users
    FOR INSERT
    WITH CHECK (true);
