-- ============================================================
-- V6: Email verification tokens.
--
-- One-time tokens emailed to users after registration. They click
-- the link in the email, which POSTs the token to /auth/verify-email,
-- which marks email_verified = TRUE and consumes the token.
-- ============================================================

CREATE TABLE verification_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(128) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Most lookups are by token value
CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);

-- Find unused tokens for a user (e.g., when re-sending verification email)
CREATE INDEX idx_verification_tokens_user_active
    ON verification_tokens(user_id)
    WHERE used_at IS NULL;