-- ============================================================
-- V5: Add email verification columns and pre-verify seeded users.
-- ============================================================

-- Add email_verified flag and timestamp
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

-- Pre-verify all seeded test users so login works immediately
UPDATE users
   SET email_verified = TRUE,
       email_verified_at = NOW()
 WHERE email IN (
   'testuser@example.com',
   'intake.officer@example.com',
   'case.officer@example.com'
 );