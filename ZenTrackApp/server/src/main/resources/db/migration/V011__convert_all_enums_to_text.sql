-- Elimina el índice parcial que referencia el tipo ENUM antes de convertir
DROP INDEX IF EXISTS idx_membership_requests_status;

-- Elimina los DEFAULT que usan literales ENUM antes de convertir el tipo
ALTER TABLE membership_requests ALTER COLUMN status    DROP DEFAULT;
ALTER TABLE sprints              ALTER COLUMN status    DROP DEFAULT;
ALTER TABLE tasks                ALTER COLUMN priority  DROP DEFAULT;

-- Convierte todas las columnas ENUM a TEXT
ALTER TABLE oauth_accounts       ALTER COLUMN provider     TYPE TEXT USING provider::TEXT;
ALTER TABLE organization_members ALTER COLUMN role          TYPE TEXT USING role::TEXT;
ALTER TABLE team_members         ALTER COLUMN role          TYPE TEXT USING role::TEXT;
ALTER TABLE workspace_members    ALTER COLUMN role          TYPE TEXT USING role::TEXT;
ALTER TABLE project_members      ALTER COLUMN role          TYPE TEXT USING role::TEXT;
ALTER TABLE membership_requests  ALTER COLUMN target_type   TYPE TEXT USING target_type::TEXT;
ALTER TABLE membership_requests  ALTER COLUMN status        TYPE TEXT USING status::TEXT;
ALTER TABLE sprints               ALTER COLUMN status        TYPE TEXT USING status::TEXT;
ALTER TABLE tasks                 ALTER COLUMN priority      TYPE TEXT USING priority::TEXT;

-- Restaura los DEFAULT ahora como literales TEXT
ALTER TABLE membership_requests ALTER COLUMN status    SET DEFAULT 'pending';
ALTER TABLE sprints              ALTER COLUMN status    SET DEFAULT 'planning';
ALTER TABLE tasks                ALTER COLUMN priority  SET DEFAULT 'medium';

-- Recrea el índice parcial con comparación TEXT
CREATE INDEX idx_membership_requests_status ON membership_requests(status) WHERE status = 'pending';
