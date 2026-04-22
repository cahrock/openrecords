-- ============================================================
-- V3: FOIA Requests Schema
-- ============================================================
-- Creates:
--   1. foia_requests            - main request table (UUID PK, state machine)
--   2. foia_request_status_history - audit trail of every status change
--   3. Seeds a test user so requests have an owner during Phase 4 dev
-- ============================================================


-- ============================================================
-- 1. foia_requests
-- ============================================================
CREATE TABLE foia_requests (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tracking_number      VARCHAR(20)   NOT NULL UNIQUE,

    -- Ownership
    requester_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    -- Request content
    subject              VARCHAR(255)  NOT NULL,
    description          TEXT          NOT NULL,
    records_requested    TEXT          NOT NULL,
    date_range_start     DATE,
    date_range_end       DATE,

    -- Workflow state (state machine)
    status               VARCHAR(50)   NOT NULL DEFAULT 'DRAFT',

    -- Fee preferences
    fee_waiver_requested BOOLEAN       NOT NULL DEFAULT FALSE,
    max_fee_willing      NUMERIC(10,2),

    -- SLA timestamps
    submitted_at         TIMESTAMPTZ,
    acknowledged_at      TIMESTAMPTZ,
    due_date             DATE,
    closed_at            TIMESTAMPTZ,

    -- Standard audit columns
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version              INTEGER       NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT foia_requests_status_check CHECK (status IN (
        'DRAFT',
        'SUBMITTED',
        'ACKNOWLEDGED',
        'ASSIGNED',
        'UNDER_REVIEW',
        'ON_HOLD',
        'RESPONSIVE_RECORDS_FOUND',
        'NO_RECORDS',
        'DOCUMENTS_RELEASED',
        'REJECTED',
        'CLOSED'
    )),
    CONSTRAINT foia_requests_date_range_check CHECK (
        date_range_start IS NULL OR date_range_end IS NULL OR date_range_start <= date_range_end
    ),
    CONSTRAINT foia_requests_max_fee_check CHECK (
        max_fee_willing IS NULL OR max_fee_willing >= 0
    )
);

-- Indexes for common query patterns
CREATE INDEX idx_foia_requests_requester ON foia_requests(requester_id);
CREATE INDEX idx_foia_requests_status ON foia_requests(status);
CREATE INDEX idx_foia_requests_submitted_at ON foia_requests(submitted_at DESC);
CREATE INDEX idx_foia_requests_due_date ON foia_requests(due_date) WHERE status NOT IN ('CLOSED', 'REJECTED', 'DOCUMENTS_RELEASED', 'NO_RECORDS');

-- Documentation
COMMENT ON TABLE  foia_requests IS 'Freedom of Information Act requests filed by citizens';
COMMENT ON COLUMN foia_requests.tracking_number IS 'Human-readable public identifier, e.g., FOIA-2026-000001';
COMMENT ON COLUMN foia_requests.status IS 'Workflow state machine value — see foia_requests_status_check constraint for allowed values';
COMMENT ON COLUMN foia_requests.due_date IS 'Statutory response deadline (20 business days after submission by default)';
COMMENT ON COLUMN foia_requests.version IS 'Optimistic locking version, managed by JPA';


-- ============================================================
-- 2. foia_request_status_history
-- ============================================================
-- Every status change on a request creates an audit row.
-- This table is append-only and never modified.

CREATE TABLE foia_request_status_history (
    id              BIGSERIAL     PRIMARY KEY,
    request_id      UUID          NOT NULL REFERENCES foia_requests(id) ON DELETE CASCADE,
    from_status     VARCHAR(50),
    to_status       VARCHAR(50)   NOT NULL,
    changed_by_id   BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    reason          TEXT,
    changed_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_status_history_request ON foia_request_status_history(request_id, changed_at DESC);

COMMENT ON TABLE foia_request_status_history IS 'Append-only audit log of all status changes on FOIA requests';


-- ============================================================
-- 3. Seed a test user for Phase 4 development
-- ============================================================
-- This is temporary scaffolding so we can create requests with a valid FK
-- before we've built authentication. We'll remove or rework this when JWT auth
-- lands in Phase 6.
--
-- Password hash below corresponds to plaintext 'password123' — we're not relying
-- on login for now, but the column is NOT NULL so we supply a valid BCrypt hash.

INSERT INTO users (email, password_hash, full_name, role, enabled)
VALUES (
    'testuser@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Test Requester',
    'REQUESTER',
    TRUE
)
ON CONFLICT (email) DO NOTHING;