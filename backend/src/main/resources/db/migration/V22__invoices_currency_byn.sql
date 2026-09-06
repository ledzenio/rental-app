UPDATE invoices SET currency = 'BYN' WHERE currency = 'RUB';

ALTER TABLE invoices ALTER COLUMN currency SET DEFAULT 'BYN';
