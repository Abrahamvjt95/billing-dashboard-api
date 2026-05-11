CREATE TYPE payment_method AS ENUM ('CASH', 'BANK_TRANSFER', 'CREDIT_CARD', 'CHECK', 'OTHER');

CREATE TABLE payments (
    id             BIGSERIAL      PRIMARY KEY,
    invoice_id     BIGINT         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount         NUMERIC(12,2)  NOT NULL,
    payment_date   DATE           NOT NULL,
    method         payment_method NOT NULL DEFAULT 'BANK_TRANSFER',
    reference      VARCHAR(255),
    notes          TEXT,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
