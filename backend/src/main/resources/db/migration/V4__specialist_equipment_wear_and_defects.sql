ALTER TABLE equipment_state_history
    ADD COLUMN calculated_wear_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN amortization_value NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN notes TEXT;

CREATE TABLE defect_reports (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    specialist_user_id BIGINT NOT NULL REFERENCES users(id),
    service_request_id BIGINT REFERENCES service_requests(id),
    defect_description TEXT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    recommended_penalty NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_defect_reports_equipment ON defect_reports(equipment_id);
CREATE INDEX idx_defect_reports_status ON defect_reports(status);
