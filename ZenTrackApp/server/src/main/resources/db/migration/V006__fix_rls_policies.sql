-- Fix 1: users — allow SELECT when app.user_id is not set (needed for login lookup).
DROP POLICY IF EXISTS users_self ON users;
DROP POLICY IF EXISTS users_self_read ON users;
DROP POLICY IF EXISTS users_self_write ON users;

CREATE POLICY users_self_read ON users
    FOR SELECT
    USING (
        current_setting('app.user_id', true) IS NULL
        OR current_setting('app.user_id', true) = ''
        OR id = current_setting('app.user_id', true)::bigint
    );

CREATE POLICY users_self_write ON users
    FOR INSERT
    WITH CHECK (true);

-- Fix 2: workspace_members — allow owner to insert themselves as OWNER on workspace creation.
DROP POLICY IF EXISTS workspace_members_access ON workspace_members;

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
        OR
        (
            user_id = current_setting('app.user_id', true)::bigint
            AND role = 'OWNER'
            AND workspace_id IN (
                SELECT id FROM workspaces
                WHERE owner_id = current_setting('app.user_id', true)::bigint
            )
        )
    );
