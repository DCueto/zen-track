CREATE TABLE IF NOT EXISTS workspace_members (
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    role         TEXT   NOT NULL DEFAULT 'MEMBER',
    PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT workspace_members_role_check CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX IF NOT EXISTS idx_workspace_members_user_id ON workspace_members(user_id);

ALTER TABLE workspace_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspace_members FORCE ROW LEVEL SECURITY;

CREATE POLICY workspace_members_access
    ON workspace_members
    USING (
        workspace_id IN (
            SELECT wm.workspace_id FROM workspace_members wm
            WHERE wm.user_id = current_setting('app.user_id', true)::bigint
        )
    )
    WITH CHECK (
        workspace_id IN (
            SELECT wm.workspace_id FROM workspace_members wm
            WHERE wm.user_id = current_setting('app.user_id', true)::bigint
              AND wm.role IN ('OWNER', 'ADMIN')
        )
    );

CREATE POLICY workspaces_member_access
    ON workspaces
    USING (
        id IN (
            SELECT workspace_id FROM workspace_members
            WHERE user_id = current_setting('app.user_id', true)::bigint
        )
    )
    WITH CHECK (
        owner_id = current_setting('app.user_id', true)::bigint
    );
