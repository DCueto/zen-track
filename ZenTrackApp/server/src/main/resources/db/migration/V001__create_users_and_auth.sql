-- Tipos ENUM
CREATE TYPE user_type AS ENUM ('regular', 'client');
CREATE TYPE oauth_provider AS ENUM ('google');

-- Tabla principal de usuarios
CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255),                         -- null si solo usa OAuth
    name           VARCHAR(255) NOT NULL,
    avatar_url     VARCHAR(500),
    user_type      user_type    NOT NULL DEFAULT 'regular',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by     BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by     BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

-- Cuentas OAuth vinculadas a un usuario
CREATE TABLE oauth_accounts (
    id                 BIGSERIAL    PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider           oauth_provider NOT NULL,
    provider_user_id   VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    access_token       TEXT,                             -- cifrado AES-256 en capa de aplicación
    refresh_token      TEXT,                             -- cifrado AES-256 en capa de aplicación
    token_expires_at   TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id)
);

-- Refresh tokens internos de ZenTrack
CREATE TABLE refresh_tokens (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,             -- SHA-256 del token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by  BIGINT      REFERENCES users(id) ON DELETE SET NULL
);

-- Índices de uso frecuente
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
