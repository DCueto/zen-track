-- Tipos ENUM
CREATE TYPE org_member_role AS ENUM ('owner', 'admin', 'member');

-- Organizaciones (empresariales y personales)
CREATE TABLE organizations (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,            -- identificador de URL (ej. 'basetis')
    plan        VARCHAR(50)  NOT NULL DEFAULT 'free',
    is_personal BOOLEAN      NOT NULL DEFAULT FALSE,     -- TRUE solo para org personal auto-creada
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

-- Miembros de una organización (solo usuarios tipo 'regular')
CREATE TABLE organization_members (
    org_id      BIGINT           NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id     BIGINT           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        org_member_role  NOT NULL,
    joined_at   TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    created_by  BIGINT           REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_by  BIGINT           REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (org_id, user_id)
);

-- Índices de uso frecuente
CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);
