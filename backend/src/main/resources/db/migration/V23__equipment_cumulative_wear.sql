-- Накопительный износ по аренде (индивидуальная норма на единицу) + журнал начислений.
ALTER TABLE equipment
    ADD COLUMN IF NOT EXISTS accumulated_wear_percent NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS rental_wear_rate_per_day NUMERIC(7, 4) NOT NULL DEFAULT 0.0800;

-- Норма «% за календарную сутку аренды» из старого base_wear_percent (осмотр), в разумных границах.
UPDATE equipment
SET rental_wear_rate_per_day = ROUND(
        LEAST(0.2800::numeric, GREATEST(0.0400::numeric, COALESCE(base_wear_percent, 5) * 0.012::numeric)),
        4
    );

ALTER TABLE defect_reports
    ADD COLUMN IF NOT EXISTS planned_extra_wear_percent NUMERIC(6, 4) NOT NULL DEFAULT 0.0000;

-- Уже существующие ведомости: надбавка по степени, кроме заключений «без дефектов».
UPDATE defect_reports
SET planned_extra_wear_percent = CASE severity
    WHEN 'LOW' THEN 0.4000
    WHEN 'MEDIUM' THEN 1.1000
    WHEN 'HIGH' THEN 2.2000
    WHEN 'CRITICAL' THEN 4.5000
    ELSE 0.0000
END
WHERE defect_description NOT ILIKE 'Дефекты не выявлены%';

UPDATE defect_reports
SET planned_extra_wear_percent = 0.0000
WHERE defect_description ILIKE 'Дефекты не выявлены%';

CREATE TABLE equipment_wear_ledger (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    entry_type VARCHAR(32) NOT NULL,
    service_request_id BIGINT REFERENCES service_requests(id),
    defect_report_id BIGINT REFERENCES defect_reports(id),
    rental_days INT,
    rental_rate_per_day NUMERIC(7, 4),
    rental_wear_delta NUMERIC(8, 4) NOT NULL DEFAULT 0,
    defect_wear_delta NUMERIC(8, 4) NOT NULL DEFAULT 0,
    total_wear_delta NUMERIC(8, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wear_ledger_equipment ON equipment_wear_ledger(equipment_id, created_at DESC);

CREATE UNIQUE INDEX uq_wear_ledger_rental_sr
    ON equipment_wear_ledger (service_request_id)
    WHERE entry_type = 'RENTAL_CLOSURE';

CREATE UNIQUE INDEX uq_wear_ledger_defect_dr
    ON equipment_wear_ledger (defect_report_id)
    WHERE entry_type = 'DEFECT_APPROVAL';
