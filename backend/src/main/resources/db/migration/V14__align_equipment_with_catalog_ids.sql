INSERT INTO categories(name)
SELECT 'Каталог аренды'
WHERE NOT EXISTS (SELECT 1 FROM categories);

INSERT INTO equipment(id, category_id, inventory_code, model_name, purchase_date, purchase_price, base_wear_percent)
SELECT s.id,
       (SELECT c.id FROM categories c ORDER BY c.id LIMIT 1),
       'CAT-' || s.id,
       s.title,
       CURRENT_DATE,
       COALESCE(s.base_price, 1000.00),
       0.00
FROM service_catalog_items s
WHERE NOT EXISTS (SELECT 1 FROM equipment e WHERE e.id = s.id);

UPDATE service_requests sr
SET equipment_id = sr.service_id
WHERE EXISTS (SELECT 1 FROM equipment e WHERE e.id = sr.service_id);

SELECT setval(
    pg_get_serial_sequence('equipment', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM equipment), 1),
    true
);
