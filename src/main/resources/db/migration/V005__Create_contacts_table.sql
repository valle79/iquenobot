-- Create contacts table
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    full_name VARCHAR(200),
    email VARCHAR(255),
    phone VARCHAR(20),
    whatsapp_phone VARCHAR(20),
    company VARCHAR(200),
    job_title VARCHAR(100),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    language VARCHAR(5) DEFAULT 'es',
    timezone VARCHAR(50) DEFAULT 'UTC',
    tags VARCHAR(500),
    custom_fields TEXT,
    notes TEXT,
    last_contacted_at TIMESTAMP,
    conversation_count INTEGER NOT NULL DEFAULT 0,
    message_count INTEGER NOT NULL DEFAULT 0,
    is_subscribed BOOLEAN NOT NULL DEFAULT TRUE,
    unsubscribed_at TIMESTAMP,
    blocked_at TIMESTAMP,
    blocked_reason VARCHAR(500),
    
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

-- Create indexes
CREATE INDEX idx_contacts_tenant_id ON contacts(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_contacts_email ON contacts(email) WHERE is_deleted = FALSE AND email IS NOT NULL;
CREATE INDEX idx_contacts_phone ON contacts(phone) WHERE is_deleted = FALSE AND phone IS NOT NULL;
CREATE INDEX idx_contacts_whatsapp_phone ON contacts(whatsapp_phone) WHERE is_deleted = FALSE AND whatsapp_phone IS NOT NULL;
CREATE INDEX idx_contacts_status ON contacts(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_contacts_full_name ON contacts(full_name) WHERE is_deleted = FALSE;
CREATE INDEX idx_contacts_company ON contacts(company) WHERE is_deleted = FALSE;
CREATE INDEX idx_contacts_last_contacted_at ON contacts(last_contacted_at);
CREATE INDEX idx_contacts_created_at ON contacts(created_at);
CREATE INDEX idx_contacts_is_deleted ON contacts(is_deleted);

-- Create composite indexes for common queries
CREATE INDEX idx_contacts_tenant_status ON contacts(tenant_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_contacts_tenant_email ON contacts(tenant_id, email) WHERE is_deleted = FALSE AND email IS NOT NULL;
CREATE INDEX idx_contacts_tenant_phone ON contacts(tenant_id, phone) WHERE is_deleted = FALSE AND phone IS NOT NULL;

-- Add constraints
ALTER TABLE contacts ADD CONSTRAINT check_contacts_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'ARCHIVED'));

-- Foreign key constraint
ALTER TABLE contacts ADD CONSTRAINT fk_contacts_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Create trigger for updated_at
CREATE TRIGGER update_contacts_updated_at BEFORE UPDATE ON contacts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();