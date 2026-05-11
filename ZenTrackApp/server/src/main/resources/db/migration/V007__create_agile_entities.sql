-- Tipos ENUM
CREATE TYPE sprint_status AS ENUM ('planning', 'active', 'closed');

-- Sprints (nivel workspace)
CREATE TABLE IF NOT EXISTS sprints (
    id           BIGSERIAL     PRIMARY KEY,
    workspace_id BIGINT        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name         VARCHAR(255)  NOT NULL,
    start_date   DATE,
    end_date     DATE,
    status       sprint_status NOT NULL DEFAULT 'planning',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by   BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by   BIGINT        REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE sprints ENABLE ROW LEVEL SECURITY;

-- Etiquetas (nivel workspace)
CREATE TABLE IF NOT EXISTS tags (
    id           BIGSERIAL    PRIMARY KEY,
    workspace_id BIGINT       NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    color_hex    VARCHAR(7),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by   BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE tags ENABLE ROW LEVEL SECURITY;

-- Estados de tarea configurables por workspace (Kanban columns)
CREATE TABLE IF NOT EXISTS task_statuses (
    id           BIGSERIAL    PRIMARY KEY,
    workspace_id BIGINT       NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    order_index  INT          NOT NULL DEFAULT 0,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by   BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE task_statuses ENABLE ROW LEVEL SECURITY;

-- Índices de uso frecuente
CREATE INDEX IF NOT EXISTS idx_sprints_workspace_id ON sprints(workspace_id);
CREATE INDEX IF NOT EXISTS idx_tags_workspace_id ON tags(workspace_id);
CREATE INDEX IF NOT EXISTS idx_task_statuses_workspace_id ON task_statuses(workspace_id);
