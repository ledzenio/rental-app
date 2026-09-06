ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(64);

UPDATE users u
SET role = 'MANAGER'
WHERE EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
      AND ur.role_name = 'MANAGER'
);

UPDATE users u
SET role = 'SERVICE_SPECIALIST'
WHERE role IS NULL
  AND EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
      AND ur.role_name = 'SERVICE_SPECIALIST'
);

UPDATE users u
SET role = 'USER'
WHERE role IS NULL
  AND EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
      AND ur.role_name = 'USER'
);

UPDATE users
SET role = 'USER'
WHERE role IS NULL;

ALTER TABLE users
    ALTER COLUMN role SET NOT NULL;

DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
