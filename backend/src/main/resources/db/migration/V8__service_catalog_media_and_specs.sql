ALTER TABLE service_catalog_items
    ADD COLUMN subtitle VARCHAR(255);

CREATE TABLE service_catalog_item_images (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES service_catalog_items(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    alt_text VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE service_catalog_item_specs (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES service_catalog_items(id) ON DELETE CASCADE,
    spec_key VARCHAR(120) NOT NULL,
    spec_value VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_service_catalog_images_service_sort ON service_catalog_item_images(service_id, sort_order, id);
CREATE INDEX idx_service_catalog_specs_service_sort ON service_catalog_item_specs(service_id, sort_order, id);

UPDATE service_catalog_items
SET subtitle = 'Каталог технической услуги'
WHERE subtitle IS NULL;

UPDATE service_catalog_items
SET category = 'Башенные краны'
WHERE title = 'Аренда перфоратора';

UPDATE service_catalog_items
SET category = 'Автомобильные краны'
WHERE title = 'Аренда генератора';

UPDATE service_catalog_items
SET category = 'Гусеничные краны'
WHERE title = 'Диагностика и обслуживание';

INSERT INTO service_catalog_items(title, subtitle, description, category, base_price, is_active)
VALUES (
    'Монтаж и проектирование',
    'ППРк, монтаж башенного крана за 1 день',
    'Полный цикл проектирования и монтажа грузоподъемной техники на объекте.',
    'Монтаж и проектирование',
    1800.00,
    TRUE
);

INSERT INTO service_catalog_item_specs(service_id, spec_key, spec_value, sort_order)
SELECT id, 'Грузоподъемность', '8 т', 0 FROM service_catalog_items WHERE title = 'Аренда перфоратора'
UNION ALL
SELECT id, 'Вылет стрелы', '42 м', 1 FROM service_catalog_items WHERE title = 'Аренда перфоратора'
UNION ALL
SELECT id, 'Высота подъема', '37 м', 2 FROM service_catalog_items WHERE title = 'Аренда перфоратора'
UNION ALL
SELECT id, 'Грузоподъемность', '80 т', 0 FROM service_catalog_items WHERE title = 'Аренда генератора'
UNION ALL
SELECT id, 'Вылет стрелы', '60 м + гусек 17,5 м', 1 FROM service_catalog_items WHERE title = 'Аренда генератора'
UNION ALL
SELECT id, 'Производитель', 'SANY Group', 2 FROM service_catalog_items WHERE title = 'Аренда генератора'
UNION ALL
SELECT id, 'Грузоподъемность', '25 т', 0 FROM service_catalog_items WHERE title = 'Диагностика и обслуживание'
UNION ALL
SELECT id, 'Вылет стрелы', '32 м', 1 FROM service_catalog_items WHERE title = 'Диагностика и обслуживание'
UNION ALL
SELECT id, 'Доп. оборудование', 'гусек 5 м', 2 FROM service_catalog_items WHERE title = 'Диагностика и обслуживание'
UNION ALL
SELECT id, 'Срок монтажа', 'от 1 дня', 0 FROM service_catalog_items WHERE title = 'Монтаж и проектирование'
UNION ALL
SELECT id, 'Документация', 'ППРк и расчеты', 1 FROM service_catalog_items WHERE title = 'Монтаж и проектирование'
UNION ALL
SELECT id, 'География', 'Вся Беларусь', 2 FROM service_catalog_items WHERE title = 'Монтаж и проектирование';

INSERT INTO service_catalog_item_images(service_id, image_url, alt_text, sort_order, is_cover)
SELECT id, 'https://images.unsplash.com/photo-1541888946425-d81bb19240f5?auto=format&fit=crop&w=1200&q=80', 'Башенный кран', 0, TRUE
FROM service_catalog_items WHERE title = 'Аренда перфоратора'
UNION ALL
SELECT id, 'https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&w=1200&q=80', 'Башенный кран на площадке', 1, FALSE
FROM service_catalog_items WHERE title = 'Аренда перфоратора'
UNION ALL
SELECT id, 'https://images.unsplash.com/photo-1581093458791-9f3c3900df4b?auto=format&fit=crop&w=1200&q=80', 'Автомобильный кран', 0, TRUE
FROM service_catalog_items WHERE title = 'Аренда генератора'
UNION ALL
SELECT id, 'https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=1200&q=80', 'Автокран на стройке', 1, FALSE
FROM service_catalog_items WHERE title = 'Аренда генератора'
UNION ALL
SELECT id, 'https://images.unsplash.com/photo-1581094794329-c8112a89af12?auto=format&fit=crop&w=1200&q=80', 'Гусеничный кран', 0, TRUE
FROM service_catalog_items WHERE title = 'Диагностика и обслуживание'
UNION ALL
SELECT id, 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1200&q=80', 'Проектирование и монтаж', 0, TRUE
FROM service_catalog_items WHERE title = 'Монтаж и проектирование';
