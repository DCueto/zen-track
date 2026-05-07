-- Fix 1: users — allow SELECT when app.user_id is not set (needed for login lookup).
-- Split into two policies so the SELECT during authentication can read the row by email.
DROP POLICY IF EXISTS users_self ON users;

CREATE POLICY users_self_read ON users
    FOR SELECT
    USING (
        -- Login path: no user_id in session yet, allow the row to be fetched for credential check
        current_setting('app.user_id', true) IS NULL
        OR current_setting('app.user_id', true) = ''
        OR id = current_setting('app.user_id', true)::uuid
    );

CREATE POLICY users_self_write ON users
    FOR INSERT
    WITH CHECK (
        current_setting('app.user_id', true) IS NOT NULL
        AND current_setting('app.user_id', true) <> ''
        AND id = current_setting('app.user_id', true)::uuid
    );

-- Fix 2: workspace_members — allow owner to insert themselves as OWNER on workspace creation
-- (bootstrap: no member row exists yet for the new workspace).
DROP POLICY IF EXISTS workspace_members_access ON workspace_members;

CREATE POLICY workspace_members_access
    ON workspace_members
    USING (
        workspace_id IN (
            SELECT wm.workspace_id FROM workspace_members wm
            WHERE wm.user_id = current_setting('app.user_id', true)::uuid
        )
    )
    WITH CHECK (
        -- Existing OWNER/ADMIN can manage members
        workspace_id IN (
            SELECT wm.workspace_id FROM workspace_members wm
            WHERE wm.user_id = current_setting('app.user_id', true)::uuid
              AND wm.role IN ('OWNER', 'ADMIN')
        )
        OR
        -- Bootstrap: workspace owner can insert themselves as OWNER before any member row exists
        (
            user_id = current_setting('app.user_id', true)::uuid
            AND role = 'OWNER'
            AND workspace_id IN (
                SELECT id FROM workspaces
                WHERE owner_id = current_setting('app.user_id', true)::uuid
            )
        )
    );
