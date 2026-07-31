-- Quote document management: metadata columns + audit history table

ALTER TABLE quotes
    ADD COLUMN discount NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN observations TEXT,
    ADD COLUMN file_name VARCHAR(255),
    ADD COLUMN file_size BIGINT,
    ADD COLUMN file_hash VARCHAR(128),
    ADD COLUMN storage_key VARCHAR(500),
    ADD COLUMN last_resent_at TIMESTAMP,
    ADD COLUMN resend_count INTEGER NOT NULL DEFAULT 0;

-- Extend status constraint with new document-management states
ALTER TABLE quotes DROP CONSTRAINT IF EXISTS check_quotes_status;
ALTER TABLE quotes ADD CONSTRAINT check_quotes_status
    CHECK (status IN ('GENERADA', 'DRAFT', 'SENT', 'REENVIADA', 'ACCEPTED', 'REJECTED', 'ANULADA', 'EXPIRED'));

-- Audit trail per quote
CREATE TABLE quote_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    quote_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    performed_by UUID,
    actor_name VARCHAR(100),
    channel VARCHAR(20),
    channel_message_id VARCHAR(100),
    details VARCHAR(1000),

    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_quote_history_tenant_quote ON quote_history(tenant_id, quote_id);
CREATE INDEX idx_quote_history_action ON quote_history(action);

ALTER TABLE quote_history ADD CONSTRAINT fk_quote_history_quote_id
    FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE CASCADE;

CREATE TRIGGER update_quote_history_updated_at BEFORE UPDATE ON quote_history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
