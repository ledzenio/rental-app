CREATE TABLE service_catalog_items (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(120) NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service_reviews (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES service_catalog_items(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saved_services (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES service_catalog_items(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, service_id)
);

CREATE TABLE service_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    service_id BIGINT NOT NULL REFERENCES service_catalog_items(id),
    notes TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO service_catalog_items(title, description, category, base_price, is_active)
VALUES
    ('Аренда перфоратора', 'Профессиональный перфоратор для строительных задач.', 'Строительное оборудование', 1200.00, TRUE),
    ('Аренда генератора', 'Бензиновый генератор 5 кВт для автономного питания.', 'Электроснабжение', 2200.00, TRUE),
    ('Диагностика и обслуживание', 'Проверка состояния оборудования сервисным специалистом.', 'Сервис', 1500.00, TRUE);

CREATE INDEX idx_service_catalog_items_category ON service_catalog_items(category);
CREATE INDEX idx_service_catalog_items_active ON service_catalog_items(is_active);
