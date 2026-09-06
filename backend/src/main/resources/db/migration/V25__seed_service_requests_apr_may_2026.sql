-- Seed service_requests test history for specific users/services.
-- Target:
--   user_id   in [11..15]
--   service_id in [40..54]
--   rental dates in [2026-04-01 .. 2026-05-03]

WITH user_pool AS (
    SELECT id
    FROM users
    WHERE id BETWEEN 11 AND 15
),
service_pool AS (
    SELECT id
    FROM service_catalog_items
    WHERE id BETWEEN 40 AND 54
),
seed_rows AS (
    SELECT
        u.id AS user_id,
        s.id AS service_id,
        row_number() OVER (ORDER BY u.id, s.id) AS rn
    FROM user_pool u
    CROSS JOIN service_pool s
),
prepared AS (
    SELECT
        sr.user_id,
        sr.service_id,
        (
            SELECT e.id
            FROM equipment e
            WHERE e.service_catalog_item_id = sr.service_id
            ORDER BY e.id
            LIMIT 1
        ) AS equipment_id,
        (DATE '2026-04-01' + (((sr.rn - 1) % 33)::INT)) AS rental_start_date,
        sr.rn,
        'Адрес объекта: Беларусь, Минск, тестовый объект #' || sr.rn
            || ' | Координаты: 53.' || lpad((500000 + sr.rn)::TEXT, 6, '0')
            || ', 27.' || lpad((300000 + sr.rn * 2)::TEXT, 6, '0')
            || ' | Дистанция от базы: ' || (12 + (sr.rn % 70)) || ' км'
            || ' | Логистическая надбавка: ' || (25 + (sr.rn % 8) * 10) || ' BYN' AS notes
    FROM seed_rows sr
)
INSERT INTO service_requests (
    user_id,
    service_id,
    equipment_id,
    notes,
    status,
    created_at,
    rental_start_date,
    rental_end_date,
    shifts_per_day,
    returned_at
)
SELECT
    p.user_id,
    p.service_id,
    p.equipment_id,
    p.notes,
    CASE
        WHEN p.rn % 6 = 1 THEN 'NEW'
        WHEN p.rn % 6 = 2 THEN 'AWAITING_PAYMENT'
        WHEN p.rn % 6 = 3 THEN 'IN_PROGRESS'
        WHEN p.rn % 6 = 4 THEN 'AWAITING_SPECIALIST_REVIEW'
        WHEN p.rn % 6 = 5 THEN 'COMPLETED'
        ELSE 'CANCELLED'
    END,
    ((p.rental_start_date)::timestamp + INTERVAL '09:00') + ((p.rn % 8) * INTERVAL '1 hour'),
    p.rental_start_date,
    LEAST(
        DATE '2026-05-03',
        p.rental_start_date + ((p.rn % 4)::INT)
    ) AS rental_end_date,
    1,
    CASE
        WHEN p.rn % 6 IN (4, 5)
            THEN LEAST(
                DATE '2026-05-03',
                p.rental_start_date + ((p.rn % 4)::INT)
            )::timestamp + INTERVAL '18:00'
        ELSE NULL
    END
FROM prepared p
WHERE p.equipment_id IS NOT NULL;

