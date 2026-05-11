-- Tipos ENUM
CREATE TYPE project_member_role AS ENUM ('admin', 'manager', 'member', 'viewer', 'client');

-- Proyectos (dentro de un workspace)
CREATE TABLE IF NOT EXISTS projects (
    id            BIGSERIAL    PRIMARY KEY,
    workspace_id  BIGINT       NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    project_key   VARCHAR(20)  NOT NULL,                 -- ej. 'ZTK', único por workspace
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_project_key_per_workspace UNIQUE (workspace_id, project_key)
);

ALTER TABLE projects ENABLE ROW LEVEL SECURITY;

-- Miembros de un proyecto (el rol puede cambiar → auditoría completa)
CREATE TABLE IF NOT EXISTS project_members (
    project_id  BIGINT              NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        project_member_role NOT NULL,
    joined_at   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    created_by  BIGINT              REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_by  BIGINT              REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (project_id, user_id)
);

ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;

-- Índices de uso frecuente
CREATE INDEX IF NOT EXISTS idx_projects_workspace_id ON projects(workspace_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id ON project_members(user_id);
