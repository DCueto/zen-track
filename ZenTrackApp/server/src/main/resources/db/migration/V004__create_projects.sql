CREATE TABLE IF NOT EXISTS projects (
    id           BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workspace_id BIGINT      NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    project_key  TEXT        NOT NULL,
    name         TEXT        NOT NULL,
    task_counter INT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT projects_key_workspace_unique UNIQUE (workspace_id, project_key)
);

CREATE INDEX IF NOT EXISTS idx_projects_workspace_id ON projects(workspace_id);

ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects FORCE ROW LEVEL SECURITY;

CREATE POLICY projects_workspace_member_access
    ON projects
    USING (
        workspace_id IN (
            SELECT workspace_id FROM workspace_members
            WHERE user_id = current_setting('app.user_id', true)::bigint
        )
    )
    WITH CHECK (
        workspace_id IN (
            SELECT workspace_id FROM workspace_members
            WHERE user_id = current_setting('app.user_id', true)::bigint
              AND role IN ('OWNER', 'ADMIN')
        )
    );
