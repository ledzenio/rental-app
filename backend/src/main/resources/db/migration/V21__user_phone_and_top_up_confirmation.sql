ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS top_up_code VARCHAR(16),
    ADD COLUMN IF NOT EXISTS top_up_code_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS pending_top_up_amount NUMERIC(12, 2);
