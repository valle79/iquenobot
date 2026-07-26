-- Create leads table
CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    source_details VARCHAR(500),
    estimated_value DECIMAL(10, 2),
    probability DECIMAL(5, 2),
    score INTEGER NOT NULL DEFAULT 0,
    assigned_to_user_id UUID,
    assigned_at TIMESTAMP,
    first_contact_at TIMESTAMP,
    last_contact_at TIMESTAMP,
    expected_close_date TIMESTAMP,
    closed_at TIMESTAMP,
    lost_reason VARCHAR(500),
    converted_to_contact_id VARCHAR(255),
    tags VARCHAR(500),
    custom_fields TEXT,
    notes TEXT,
    
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
CREATE INDEX idx_leads_tenant_id ON leads(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_contact_id ON leads(contact_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_status ON leads(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_source ON leads(source) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_assigned_to_user_id ON leads(assigned_to_user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_score ON leads(score) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_created_at ON leads(created_at);
CREATE INDEX idx_leads_last_contact_at ON leads(last_contact_at) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_closed_at ON leads(closed_at) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_is_deleted ON leads(is_deleted);

-- Create composite indexes for common queries
CREATE INDEX idx_leads_tenant_status ON leads(tenant_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_tenant_assigned ON leads(tenant_id, assigned_to_user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_tenant_source ON leads(tenant_id, source) WHERE is_deleted = FALSE;
CREATE INDEX idx_leads_tenant_contact ON leads(tenant_id, contact_id) WHERE is_deleted = FALSE;

-- Add constraints
ALTER TABLE leads ADD CONSTRAINT check_leads_status 
    CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CONVERTED', 'LOST', 'DISQUALIFIED'));

ALTER TABLE leads ADD CONSTRAINT check_leads_source 
    CHECK (source IN ('WHATSAPP', 'WEB_FORM', 'PHONE', 'EMAIL', 'SOCIAL_MEDIA', 'REFERRAL', 'ADVERTISING', 'EVENT', 'DIRECT', 'OTHER'));

ALTER TABLE leads ADD CONSTRAINT check_leads_score 
    CHECK (score >= 0 AND score <= 100);

ALTER TABLE leads ADD CONSTRAINT check_leads_probability 
    CHECK (probability IS NULL OR (probability >= 0 AND probability <= 100));

-- Foreign key constraints
ALTER TABLE leads ADD CONSTRAINT fk_leads_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE leads ADD CONSTRAINT fk_leads_contact_id 
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE;

ALTER TABLE leads ADD CONSTRAINT fk_leads_assigned_to_user_id 
    FOREIGN KEY (assigned_to_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Create trigger for updated_at
CREATE TRIGGER update_leads_updated_at BEFORE UPDATE ON leads 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
