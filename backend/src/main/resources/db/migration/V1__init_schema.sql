CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE equipment (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    inventory_code VARCHAR(80) NOT NULL UNIQUE,
    model_name VARCHAR(255) NOT NULL,
    purchase_date DATE NOT NULL,
    purchase_price NUMERIC(14, 2) NOT NULL,
    base_wear_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE equipment_state_history (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    condition_label VARCHAR(64) NOT NULL,
    battery_percent SMALLINT,
    temperature_celsius NUMERIC(5, 2),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rentals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE rental_items (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL REFERENCES rentals(id) ON DELETE CASCADE,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    daily_rate NUMERIC(12, 2) NOT NULL,
    calculated_wear_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00
);

INSERT INTO roles(name) VALUES ('USER'), ('MANAGER'), ('SERVICE_SPECIALIST');

-- demo seeded staff users, password for both: password
INSERT INTO users(email, password_hash, full_name, status)
VALUES
    ('manager@rental.local', '$2a$10$N9qo8uLOickgx2ZMRZo5i.4jQfQ5myP2Y1r83DNCDOr3LxaVTm160', 'System Manager', 'ACTIVE'),
    ('specialist@rental.local', '$2a$10$N9qo8uLOickgx2ZMRZo5i.4jQfQ5myP2Y1r83DNCDOr3LxaVTm160', 'Service Specialist', 'ACTIVE');

INSERT INTO user_roles(user_id, role_name)
SELECT id, 'MANAGER' FROM users WHERE email = 'manager@rental.local';

INSERT INTO user_roles(user_id, role_name)
SELECT id, 'SERVICE_SPECIALIST' FROM users WHERE email = 'specialist@rental.local';

CREATE INDEX idx_equipment_category ON equipment(category_id);
CREATE INDEX idx_state_history_equipment_recorded ON equipment_state_history(equipment_id, recorded_at DESC);
CREATE INDEX idx_rental_user ON rentals(user_id);
