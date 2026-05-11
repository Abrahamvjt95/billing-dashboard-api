CREATE TYPE invoice_status AS ENUM ('DRAFT', 'SENT', 'PAID', 'OVERDUE');

CREATE TABLE invoices (
    id           BIGSERIAL      PRIMARY KEY,
    owner_id     BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id    BIGINT         NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    invoice_number VARCHAR(50)  NOT NULL,
    status       invoice_status NOT NULL DEFAULT 'DRAFT',
    issue_date   DATE           NOT NULL,
    due_date     DATE           NOT NULL,
    notes        TEXT,
    subtotal     NUMERIC(12,2)  NOT NULL DEFAULT 0,
    tax_rate     NUMERIC(5,2)   NOT NULL DEFAULT 0,
    tax_amount   NUMERIC(12,2)  NOT NULL DEFAULT 0,
    total        NUMERIC(12,2)  NOT NULL DEFAULT 0,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    UNIQUE(owner_id, invoice_number)
);

CREATE TABLE invoice_items (
    id          BIGSERIAL     PRIMARY KEY,
    invoice_id  BIGINT        NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500)  NOT NULL,
    quantity    NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price  NUMERIC(12,2) NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoices_owner_id  ON invoices(owner_id);
CREATE INDEX idx_invoices_client_id ON invoices(client_id);
CREATE INDEX idx_invoices_status    ON invoices(status);
CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
