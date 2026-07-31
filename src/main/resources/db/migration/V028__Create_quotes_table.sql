-- Create quotes table
CREATE SEQUENCE IF NOT EXISTS quote_number_seq START WITH 1;

CREATE TABLE quotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    conversation_id UUID,
    quote_number VARCHAR(30) NOT NULL,
    items TEXT NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
    igv NUMERIC(12,2) NOT NULL DEFAULT 0,
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency VARCHAR(5) NOT NULL DEFAULT 'PEN',
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    pdf_url VARCHAR(1000),

    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,

    -- Soft delete fields
    deleted_at TIMESTAMP,
    deleted_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_quotes_tenant_id ON quotes(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_quotes_contact_id ON quotes(contact_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_quotes_conversation_id ON quotes(conversation_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_quotes_quote_number ON quotes(quote_number) WHERE is_deleted = FALSE;
CREATE INDEX idx_quotes_created_at ON quotes(created_at);

ALTER TABLE quotes ADD CONSTRAINT check_quotes_status
    CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED'));

ALTER TABLE quotes ADD CONSTRAINT fk_quotes_tenant_id
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE quotes ADD CONSTRAINT fk_quotes_contact_id
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE;

ALTER TABLE quotes ADD CONSTRAINT fk_quotes_conversation_id
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL;

CREATE TRIGGER update_quotes_updated_at BEFORE UPDATE ON quotes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
