-- ============================================================
-- V4 — Add assignee, business indexes, and seed staff users
-- ============================================================

-- 1) Assignee column on foia_requests
ALTER TABLE foia_requests
    ADD COLUMN IF NOT EXISTS assignee_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- 2) Indexes for common queue queries
CREATE INDEX IF NOT EXISTS idx_foia_requests_assignee
    ON foia_requests (assignee_id)
    WHERE assignee_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_foia_requests_status
    ON foia_requests (status);

-- 3) Seed two staff users so we have real assignees in dev.
--    Password hash is BCrypt of 'password123'. Dev only.
INSERT INTO users (email, password_hash, full_name, role)
VALUES
    ('intake.officer@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Intake Officer',
     'STAFF'),
    ('case.officer@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Case Officer',
     'STAFF')
ON CONFLICT (email) DO NOTHING;