ALTER TABLE users
    ADD COLUMN virtual_balance NUMERIC(12, 2) NOT NULL DEFAULT 1500.00;

ALTER TABLE invoices
    ADD COLUMN payment_code VARCHAR(16),
    ADD COLUMN payment_code_expires_at TIMESTAMP;
