ALTER TABLE equipment
    ADD COLUMN lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE';

CREATE INDEX idx_equipment_lifecycle_status ON equipment(lifecycle_status);
