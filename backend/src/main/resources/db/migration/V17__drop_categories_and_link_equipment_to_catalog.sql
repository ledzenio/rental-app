ALTER TABLE equipment
    ADD COLUMN IF NOT EXISTS service_catalog_item_id BIGINT REFERENCES service_catalog_items(id);

UPDATE equipment e
SET service_catalog_item_id = e.id
WHERE e.service_catalog_item_id IS NULL
  AND EXISTS (SELECT 1 FROM service_catalog_items s WHERE s.id = e.id);

UPDATE equipment e
SET service_catalog_item_id = s.id
FROM service_catalog_items s
WHERE e.service_catalog_item_id IS NULL
  AND LOWER(TRIM(e.model_name)) = LOWER(TRIM(s.title));

CREATE INDEX IF NOT EXISTS idx_equipment_service_catalog_item_id ON equipment(service_catalog_item_id);

DROP INDEX IF EXISTS idx_equipment_category;
ALTER TABLE equipment DROP CONSTRAINT IF EXISTS equipment_category_id_fkey;
ALTER TABLE equipment DROP COLUMN IF EXISTS category_id;
DROP TABLE IF EXISTS categories;
