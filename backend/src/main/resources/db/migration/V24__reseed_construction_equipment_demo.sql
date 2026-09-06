CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Safe reseed mode:
-- - preserves existing history (service_requests, invoices, defect_reports, wear/history)
-- - rewrites only the active catalog and appends equipment for new catalog positions

-- 1) Optional demo users for richer UI datasets (does not touch existing users/history).
INSERT INTO users (
    email,
    password_hash,
    full_name,
    status,
    created_at,
    virtual_balance,
    role
)
SELECT
    seed.email,
    crypt('password', gen_salt('bf', 10)),
    seed.full_name,
    'ACTIVE',
    CURRENT_TIMESTAMP - seed.created_days_ago * INTERVAL '1 day',
    seed.virtual_balance,
    'USER'
FROM (
    VALUES
        ('demo.user1@rental.local', 'Иван Ковалев', 45, 2350.00::NUMERIC),
        ('demo.user2@rental.local', 'Мария Ермак', 38, 1840.00::NUMERIC),
        ('demo.user3@rental.local', 'Алексей Синкевич', 29, 1640.00::NUMERIC),
        ('demo.user4@rental.local', 'Ольга Новик', 21, 1220.00::NUMERIC),
        ('demo.user5@rental.local', 'Дмитрий Гринь', 17, 980.00::NUMERIC)
) AS seed(email, full_name, created_days_ago, virtual_balance)
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = seed.email
);

-- 2) Deactivate previous active catalog positions (history remains linked to old rows).
UPDATE service_catalog_items
SET is_active = FALSE
WHERE is_active = TRUE;

-- 3) New active catalog: only rental equipment categories.
INSERT INTO service_catalog_items(title, subtitle, description, category, base_price, is_active)
VALUES
    ('Мини-экскаватор JCB 8026 CTS', '2.8 т · для стесненных площадок', 'Компактный экскаватор для земляных работ в плотной городской застройке и внутри дворов.', 'Экскаваторы', 620.00, TRUE),
    ('Гусеничный экскаватор CAT 320', '20 т · универсальный землеройный класс', 'Надежный экскаватор для котлованов, траншей и подготовки площадок.', 'Экскаваторы', 980.00, TRUE),
    ('Колесный экскаватор Hyundai HW140', 'Маневренная техника для города', 'Экскаватор для работ с частыми перемещениями между объектами.', 'Экскаваторы', 910.00, TRUE),
    ('Фронтальный погрузчик CAT 950', 'Объем ковша 3.2 м3', 'Погрузка инертных материалов, планировка территории, перемещение грунта.', 'Погрузчики', 890.00, TRUE),
    ('Телескопический погрузчик JCB 531-70', 'Высота подъема до 7 м', 'Универсальный погрузчик для строительных и складских задач на объекте.', 'Погрузчики', 760.00, TRUE),
    ('Мини-погрузчик Bobcat S650', 'Компактный формат для точечных работ', 'Многоцелевая машина для благоустройства и коротких циклов погрузки.', 'Погрузчики', 540.00, TRUE),
    ('Дизель-генератор FG Wilson 50 кВт', 'Резервное питание стройплощадки', 'Стабильное питание временных строительных сетей и бытовых городков.', 'Генераторы', 480.00, TRUE),
    ('Дизель-генератор Atlas Copco 100 кВт', 'Для крупных объектов', 'Энергоснабжение монтажных работ и интенсивных потребителей.', 'Генераторы', 790.00, TRUE),
    ('Инверторный генератор 10 кВт', 'Низкий уровень шума', 'Локальное питание для отделочных и сервисных работ.', 'Генераторы', 260.00, TRUE),
    ('Виброплита Wacker Neuson DPU5545', 'Глубина уплотнения до 40 см', 'Уплотнение основания под тротуары, дорожки и фундаментные плиты.', 'Уплотнение и бетон', 190.00, TRUE),
    ('Виброкаток Ammann ARX 26', 'Для асфальта и подстилающих слоев', 'Компактный каток для дорожных и благоустроительных работ.', 'Уплотнение и бетон', 430.00, TRUE),
    ('Глубинный вибратор Enar Dingo', 'Укладка монолитного бетона', 'Удаление воздушных полостей и повышение плотности бетона.', 'Уплотнение и бетон', 140.00, TRUE),
    ('Ножничный подъемник Genie GS-3246', 'Рабочая высота до 12 м', 'Безопасный доступ для монтажных и фасадных работ на высоте.', 'Подъемная техника', 520.00, TRUE),
    ('Коленчатый подъемник JLG 450AJ', 'Работа с вылетом стрелы', 'Гибкое позиционирование корзины в сложной геометрии объекта.', 'Подъемная техника', 680.00, TRUE),
    ('Мачтовый подъемник Haulotte Star 10', 'Компактный для внутренних работ', 'Точечный доступ на высоте в ограниченном пространстве.', 'Подъемная техника', 360.00, TRUE);

-- 4) Specs for new rows.
INSERT INTO service_catalog_item_specs(service_id, spec_key, spec_value, sort_order)
SELECT id, 'Рабочая масса', '2.8–20 т', 0
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Экскаваторы'
UNION ALL
SELECT id, 'Глубина копания', 'до 6.7 м', 1
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Экскаваторы'
UNION ALL
SELECT id, 'Тип хода', 'гусеничный / колесный', 2
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Экскаваторы'
UNION ALL
SELECT id, 'Грузоподъемность', '1.2–5.0 т', 0
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Погрузчики'
UNION ALL
SELECT id, 'Объем ковша', '0.6–3.2 м3', 1
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Погрузчики'
UNION ALL
SELECT id, 'Навесное оборудование', 'ковш / вилы / щетка', 2
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Погрузчики'
UNION ALL
SELECT id, 'Мощность', '10–100 кВт', 0
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Генераторы'
UNION ALL
SELECT id, 'Топливо', 'дизель / бензин', 1
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Генераторы'
UNION ALL
SELECT id, 'Время автономной работы', '8–24 ч', 2
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Генераторы'
UNION ALL
SELECT id, 'Тип работ', 'уплотнение и бетон', 0
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Уплотнение и бетон'
UNION ALL
SELECT id, 'Рабочая ширина', '350–1200 мм', 1
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Уплотнение и бетон'
UNION ALL
SELECT id, 'Производительность', 'до 900 м2/смена', 2
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Уплотнение и бетон'
UNION ALL
SELECT id, 'Рабочая высота', '8–16 м', 0
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Подъемная техника'
UNION ALL
SELECT id, 'Тип питания', 'электро / дизель', 1
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Подъемная техника'
UNION ALL
SELECT id, 'Грузоподъемность платформы', '200–450 кг', 2
FROM service_catalog_items
WHERE is_active = TRUE AND category = 'Подъемная техника';

-- 5) Images for new active positions.
INSERT INTO service_catalog_item_images(service_id, image_url, alt_text, sort_order, is_cover)
SELECT id, 'https://placehold.co/1200x700?text=' || replace(title, ' ', '+'), title || ' (обложка)', 0, TRUE
FROM service_catalog_items
WHERE is_active = TRUE
UNION ALL
SELECT id, 'https://placehold.co/1200x700?text=' || replace(title, ' ', '+') || '+gallery', title || ' (галерея)', 1, FALSE
FROM service_catalog_items
WHERE is_active = TRUE;

-- 6) Add equipment units only for current active catalog; keep old equipment unchanged for history integrity.
INSERT INTO equipment (
    inventory_code,
    model_name,
    purchase_date,
    purchase_price,
    base_wear_percent,
    service_catalog_item_id,
    lifecycle_status,
    accumulated_wear_percent,
    rental_wear_rate_per_day
)
SELECT
    format('CNSTR-%s-%s', lpad(s.id::TEXT, 3, '0'), unit_no) AS inventory_code,
    s.title || ' · Ед. ' || unit_no AS model_name,
    CURRENT_DATE - ((s.id * 11 + unit_no * 7)::INT),
    ROUND((s.base_price * 82 + unit_no * 140)::NUMERIC, 2),
    ROUND((4 + ((s.id + unit_no) % 7))::NUMERIC, 2),
    s.id,
    CASE
        WHEN (s.id + unit_no) % 17 = 0 THEN 'UNDER_REPAIR'
        WHEN (s.id + unit_no) % 7 = 0 THEN 'NEEDS_REPAIR'
        ELSE 'AVAILABLE'
    END,
    ROUND((8 + ((s.id * unit_no) % 28))::NUMERIC, 2),
    ROUND((0.06 + ((s.id % 5) * 0.02) + (unit_no * 0.005))::NUMERIC, 4)
FROM service_catalog_items s
CROSS JOIN generate_series(1, 2) AS g(unit_no)
WHERE s.is_active = TRUE;

-- 7) Seed initial inspection history for newly added units only.
INSERT INTO equipment_state_history (
    equipment_id,
    condition_label,
    battery_percent,
    temperature_celsius,
    calculated_wear_percent,
    amortization_value,
    notes,
    recorded_at
)
SELECT
    e.id,
    CASE
        WHEN wear_snapshot <= 20 THEN 'ОТЛИЧНОЕ'
        WHEN wear_snapshot <= 45 THEN 'ХОРОШЕЕ'
        WHEN wear_snapshot <= 70 THEN 'УДОВЛЕТВОРИТЕЛЬНОЕ'
        WHEN wear_snapshot <= 85 THEN 'ПЛОХОЕ'
        ELSE 'АВАРИЙНОЕ'
    END,
    GREATEST(20, 95 - (seq_no * 12)),
    ROUND((35 + seq_no * 3 + (e.id % 5))::NUMERIC, 1),
    wear_snapshot,
    ROUND((e.purchase_price * wear_snapshot / 100.0)::NUMERIC, 2),
    'Плановый техосмотр №' || seq_no,
    CURRENT_TIMESTAMP - ((4 - seq_no) * 8 + (e.id % 5)) * INTERVAL '1 day'
FROM equipment e
CROSS JOIN generate_series(1, 3) AS seq(seq_no)
CROSS JOIN LATERAL (
    SELECT ROUND(GREATEST(2, LEAST(97, e.accumulated_wear_percent - (3 - seq.seq_no) * 2.5))::NUMERIC, 2) AS wear_snapshot
) ws
WHERE e.inventory_code LIKE 'CNSTR-%';

