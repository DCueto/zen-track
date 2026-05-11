-- Tipos ENUM
CREATE TYPE workspace_member_role AS ENUM ('admin', 'manager', 'member', 'client');

-- Workspaces (espacios de trabajo dentro de una organización)
CREATE TABLE IF NOT EXISTS workspaces (
    id          BIGSERIAL    PRIMARY KEY,
    org_id      BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE workspaces ENABLE ROW LEVEL SECURITY;

-- N:M workspace ↔ team (equipo asignado a un workspace; sin campos mutables → solo auditoría de creación)
CREATE TABLE IF NOT EXISTS workspace_teams (
    workspace_id BIGINT      NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    team_id      BIGINT      NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (workspace_id, team_id)
);

ALTER TABLE workspace_teams ENABLE ROW LEVEL SECURITY;

-- Miembros directos de un workspace (el rol puede cambiar → auditoría completa)
CREATE TABLE IF NOT EXISTS workspace_members (
    workspace_id BIGINT                NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      BIGINT                NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role         workspace_member_role NOT NULL,
    joined_at    TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    created_by   BIGINT                REFERENCES users(id) ON DELETE SET NULL,
    updated_at   TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    updated_by   BIGINT                REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (workspace_id, user_id)
);

ALTER TABLE workspace_members ENABLE ROW LEVEL SECURITY;

-- Índices de uso frecuente
CREATE INDEX IF NOT EXISTS idx_workspaces_org_id ON workspaces(org_id);
CREATE INDEX IF NOT EXISTS idx_workspace_teams_team_id ON workspace_teams(team_id);
CREATE INDEX IF NOT EXISTS idx_workspace_members_user_id ON workspace_members(user_id);
