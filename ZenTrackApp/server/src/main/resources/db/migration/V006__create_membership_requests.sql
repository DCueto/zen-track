-- Tipos ENUM
CREATE TYPE membership_target_type AS ENUM ('organization', 'team', 'workspace');
CREATE TYPE membership_request_status AS ENUM ('pending', 'approved', 'rejected');

-- Solicitudes de membresía (a org, team o workspace)
CREATE TABLE IF NOT EXISTS membership_requests (
    id           BIGSERIAL                  PRIMARY KEY,
    requester_id BIGINT                     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type  membership_target_type     NOT NULL,
    target_id    BIGINT                     NOT NULL,
    status       membership_request_status  NOT NULL DEFAULT 'pending',
    reviewed_by  BIGINT                     REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ                NOT NULL DEFAULT NOW(),
    created_by   BIGINT                     REFERENCES users(id) ON DELETE SET NULL,
    updated_at   TIMESTAMPTZ                NOT NULL DEFAULT NOW(),
    updated_by   BIGINT                     REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE membership_requests ENABLE ROW LEVEL SECURITY;

-- Índices de uso frecuente
CREATE INDEX IF NOT EXISTS idx_membership_requests_requester_id ON membership_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_membership_requests_target ON membership_requests(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_membership_requests_status ON membership_requests(status) WHERE status = 'pending';
