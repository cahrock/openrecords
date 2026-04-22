-- Enable the pgcrypto extension for gen_random_uuid() function
-- Required before creating tables that use UUID primary keys.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

COMMENT ON EXTENSION pgcrypto IS 'Cryptographic functions including gen_random_uuid() for UUID v4 generation';