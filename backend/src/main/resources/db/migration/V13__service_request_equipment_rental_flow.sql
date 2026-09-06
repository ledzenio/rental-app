ALTER TABLE service_requests
    ADD COLUMN equipment_id BIGINT REFERENCES equipment(id),
    ADD COLUMN rental_start_date DATE,
    ADD COLUMN rental_end_date DATE,
    ADD COLUMN shifts_per_day INT,
    ADD COLUMN returned_at TIMESTAMP;

INSERT INTO categories(name)
SELECT 'Резервная категория'
WHERE NOT EXISTS (SELECT 1 FROM categories);

INSERT INTO equipment(category_id, inventory_code, model_name, purchase_date, purchase_price, base_wear_percent)
SELECT c.id, 'TMP-RESERVE-001', 'Резервная единица парка', CURRENT_DATE, 1000.00, 0.00
FROM categories c
ORDER BY c.id
LIMIT 1
ON CONFLICT (inventory_code) DO NOTHING;

UPDATE service_requests sr
SET equipment_id = COALESCE(sr.equipment_id, (
        SELECT e.id
        FROM equipment e
        ORDER BY e.id
        LIMIT 1
    )),
    rental_start_date = CURRENT_DATE,
    rental_end_date = CURRENT_DATE + 1,
    shifts_per_day = 1
WHERE sr.equipment_id IS NULL;

ALTER TABLE service_requests
    ALTER COLUMN equipment_id SET NOT NULL,
    ALTER COLUMN rental_start_date SET NOT NULL,
    ALTER COLUMN rental_end_date SET NOT NULL,
    ALTER COLUMN shifts_per_day SET NOT NULL;

CREATE INDEX idx_service_requests_equipment ON service_requests(equipment_id);
CREATE INDEX idx_service_requests_rental_dates ON service_requests(rental_start_date, rental_end_date);
