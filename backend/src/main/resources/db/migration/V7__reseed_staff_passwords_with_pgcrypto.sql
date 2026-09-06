CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Recreate valid bcrypt hashes for seeded staff accounts.
UPDATE users
SET password_hash = crypt('password', gen_salt('bf', 10))
WHERE email IN ('manager@rental.local', 'specialist@rental.local');
