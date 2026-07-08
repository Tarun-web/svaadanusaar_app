-- Add missing email verification columns introduced in entity
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS email_verification_token text,
    ADD COLUMN IF NOT EXISTS email_verification_token_expiry timestamptz;

