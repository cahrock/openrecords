-- Initial schema for OpenRecords FOIA Portal
-- V1: Users table (minimal version - will expand in later migrations)

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'REQUESTER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT users_role_check CHECK (role IN ('REQUESTER', 'STAFF', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

COMMENT ON TABLE users IS 'Users of the FOIA portal - both citizens (requesters) and agency staff';
COMMENT ON COLUMN users.role IS 'REQUESTER = citizen filing FOIA requests, STAFF = agency case officer, ADMIN = portal administrator';