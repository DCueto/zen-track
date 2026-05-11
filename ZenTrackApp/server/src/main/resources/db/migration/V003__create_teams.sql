-- Tipos ENUM
CREATE TYPE team_member_role AS ENUM ('admin', 'manager', 'member');

-- Teams (departamentos dentro de una organización)
CREATE TABLE IF NOT EXISTS teams (
    id          BIGSERIAL    PRIMARY KEY,
    org_id      BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    color_hex   VARCHAR(7),                              -- ej. '#3B82F6', nullable
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE teams ENABLE ROW LEVEL SECURITY;

-- Miembros de un team (solo usuarios tipo 'regular' que pertenezcan a la org)
CREATE TABLE IF NOT EXISTS team_members (
    team_id     BIGINT           NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id     BIGINT           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        team_member_role NOT NULL,
    joined_at   TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    created_by  BIGINT           REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_by  BIGINT           REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (team_id, user_id)
);

ALTER TABLE team_members ENABLE ROW LEVEL SECURITY;

-- Índices de uso frecuente
CREATE INDEX IF NOT EXISTS idx_teams_org_id ON teams(org_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user_id ON team_members(user_id);
