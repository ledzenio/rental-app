-- BCrypt hash for plain password: "password"
UPDATE users
SET password_hash = '$2a$10$7EqJtq98hPqEX7fNZaFWoOeR6jQ9fQ4f6Y4ZC/7gv2Fne5DE5ZT6.'
WHERE email IN ('manager@rental.local', 'specialist@rental.local');
