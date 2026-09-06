UPDATE service_requests
SET status = 'NEW'
WHERE status = 'AWAITING_FEEDBACK';
