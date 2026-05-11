-- Tipos ENUM
CREATE TYPE task_priority AS ENUM ('low', 'medium', 'high', 'critical');

-- Tareas (dentro de un proyecto)
CREATE TABLE IF NOT EXISTS tasks (
    id              BIGSERIAL     PRIMARY KEY,
    project_id      BIGINT        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    sprint_id       BIGINT        REFERENCES sprints(id) ON DELETE SET NULL,
    parent_id       BIGINT        REFERENCES tasks(id) ON DELETE SET NULL,
    task_number     INT           NOT NULL,                -- autoincremental por proyecto
    display_id      VARCHAR(50)   NOT NULL,                -- inmutable: [PROJECT_KEY]-[N]
    title           VARCHAR(255)  NOT NULL,
    description     TEXT,
    status_id       BIGINT        REFERENCES task_statuses(id) ON DELETE SET NULL,
    priority        task_priority NOT NULL DEFAULT 'medium',
    estimate        INT,                                   -- en horas, nullable
    start_date      DATE,
    due_date        DATE,
    git_branch_name VARCHAR(255),                         -- null si borrador o fallo Git
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by      BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_task_number_per_project UNIQUE (project_id, task_number),
    CONSTRAINT uq_display_id_per_project  UNIQUE (project_id, display_id)
);

ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

-- Asignados a una tarea (N:M, sin campos mutables)
CREATE TABLE IF NOT EXISTS task_assignees (
    task_id    BIGINT      NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (task_id, user_id)
);

ALTER TABLE task_assignees ENABLE ROW LEVEL SECURITY;

-- Etiquetas de una tarea (N:M, sin campos mutables)
CREATE TABLE IF NOT EXISTS task_tags (
    task_id    BIGINT      NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id     BIGINT      NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (task_id, tag_id)
);

ALTER TABLE task_tags ENABLE ROW LEVEL SECURITY;

-- Índices de uso frecuente
CREATE INDEX IF NOT EXISTS idx_tasks_project_id   ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_sprint_id    ON tasks(sprint_id);
CREATE INDEX IF NOT EXISTS idx_tasks_parent_id    ON tasks(parent_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status_id    ON tasks(status_id);
CREATE INDEX IF NOT EXISTS idx_task_assignees_user_id ON task_assignees(user_id);
CREATE INDEX IF NOT EXISTS idx_task_tags_tag_id       ON task_tags(tag_id);
