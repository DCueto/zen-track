CREATE TABLE IF NOT EXISTS project_members (
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    role       TEXT   NOT NULL DEFAULT 'MEMBER',
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT project_members_role_check CHECK (role IN ('LEAD', 'MEMBER'))
);

CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id    ON project_members(user_id);

ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_members FORCE ROW LEVEL SECURITY;

CREATE POLICY project_members_access
    ON project_members
    USING (
        project_id IN (
            SELECT p.id FROM projects p
            WHERE p.workspace_id IN (
                SELECT workspace_id FROM workspace_members
                WHERE user_id = current_setting('app.user_id', true)::bigint
            )
        )
    )
    WITH CHECK (
        project_id IN (
            SELECT p.id FROM projects p
            WHERE p.workspace_id IN (
                SELECT workspace_id FROM workspace_members
                WHERE user_id = current_setting('app.user_id', true)::bigint
                  AND role IN ('OWNER', 'ADMIN')
            )
        )
    );
