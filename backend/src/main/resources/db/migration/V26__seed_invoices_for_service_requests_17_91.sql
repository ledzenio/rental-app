-- Seed service invoices for service_request_id 17..91.
-- Adds one SERVICE invoice per request only if it does not exist yet.

INSERT INTO invoices (
    user_id,
    service_request_id,
    defect_report_id,
    invoice_type,
    amount,
    currency,
    status,
    description,
    created_at,
    paid_at,
    payment_code,
    payment_code_expires_at
)
SELECT
    sr.user_id,
    sr.id,
    NULL,
    'SERVICE',
    ROUND((
        s.base_price * ((sr.rental_end_date - sr.rental_start_date) + 1)
        + (30 + (sr.id % 10) * 12)
    )::NUMERIC, 2),
    'BYN',
    CASE
        WHEN sr.status IN ('NEW', 'AWAITING_PAYMENT') THEN 'ISSUED'
        ELSE 'PAID'
    END,
    'Счет за аренду техники "' || s.title || '" по заявке #' || sr.id,
    COALESCE(sr.created_at, CURRENT_TIMESTAMP) + INTERVAL '2 hour',
    CASE
        WHEN sr.status IN ('NEW', 'AWAITING_PAYMENT') THEN NULL
        ELSE COALESCE(sr.created_at, CURRENT_TIMESTAMP) + INTERVAL '1 day'
    END,
    CASE
        WHEN sr.status IN ('NEW', 'AWAITING_PAYMENT')
            THEN LPAD((200000 + sr.id)::TEXT, 6, '0')
        ELSE NULL
    END,
    CASE
        WHEN sr.status IN ('NEW', 'AWAITING_PAYMENT')
            THEN COALESCE(sr.created_at, CURRENT_TIMESTAMP) + INTERVAL '2 day'
        ELSE NULL
    END
FROM service_requests sr
JOIN service_catalog_items s ON s.id = sr.service_id
WHERE sr.id BETWEEN 17 AND 91
  AND NOT EXISTS (
      SELECT 1
      FROM invoices i
      WHERE i.service_request_id = sr.id
        AND i.invoice_type = 'SERVICE'
  );

