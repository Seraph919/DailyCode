ALTER TABLE leetcode_accounts
    ADD COLUMN verification_expires_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_leetcode_accounts_challenge_expiry
        CHECK (verification_expires_at IS NULL
            OR (verification_token IS NOT NULL AND verified = FALSE));

-- Existing tokens without an expiry remain stored, but are inactive in Java.
-- Time passing is checked by the application, not by a database CHECK constraint.
