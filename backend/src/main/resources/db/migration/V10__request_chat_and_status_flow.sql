ALTER TABLE service_requests
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE service_request_messages (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    sender_role VARCHAR(32) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    message_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_request_messages_request_created
    ON service_request_messages(request_id, created_at DESC);
