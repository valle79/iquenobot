-- Create tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    subdomain VARCHAR(50) NOT NULL UNIQUE,
    contact_email VARCHAR(255) NOT NULL UNIQUE,
    contact_phone VARCHAR(20),
    website_url VARCHAR(500),
    logo_url VARCHAR(500),
    address VARCHAR(500),
    city VARCHAR(100),
    country VARCHAR(100),
    timezone VARCHAR(50) DEFAULT 'UTC',
    currency VARCHAR(5) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    subscription_plan VARCHAR(50),
    subscription_expires_at DATE,
    max_users INTEGER,
    max_conversations INTEGER,
    features TEXT,
    
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
CREATE INDEX idx_tenants_subdomain ON tenants(subdomain) WHERE is_deleted = FALSE;
CREATE INDEX idx_tenants_contact_email ON tenants(contact_email) WHERE is_deleted = FALSE;
CREATE INDEX idx_tenants_status ON tenants(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_tenants_created_at ON tenants(created_at);
CREATE INDEX idx_tenants_is_deleted ON tenants(is_deleted);

-- Add constraints
ALTER TABLE tenants ADD CONSTRAINT check_tenants_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'TRIAL', 'EXPIRED'));

-- Create trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();