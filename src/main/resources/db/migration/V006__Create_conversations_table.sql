-- Create conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    assigned_user_id UUID,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    subject VARCHAR(200),
    channel_conversation_id VARCHAR(100),
    last_message_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_response_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    response_time_seconds BIGINT,
    resolution_time_seconds BIGINT,
    message_count INTEGER NOT NULL DEFAULT 0,
    unread_count INTEGER NOT NULL DEFAULT 0,
    satisfaction_rating INTEGER,
    satisfaction_feedback VARCHAR(1000),
    tags VARCHAR(500),
    metadata TEXT,
    is_bot_conversation BOOLEAN NOT NULL DEFAULT FALSE,
    bot_handoff_at TIMESTAMP,
    
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
CREATE INDEX idx_conversations_tenant_id ON conversations(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_contact_id ON conversations(contact_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_assigned_user_id ON conversations(assigned_user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_channel ON conversations(channel) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_status ON conversations(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_priority ON conversations(priority) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_last_message_at ON conversations(last_message_at);
CREATE INDEX idx_conversations_created_at ON conversations(created_at);
CREATE INDEX idx_conversations_is_deleted ON conversations(is_deleted);
CREATE INDEX idx_conversations_channel_conversation_id ON conversations(channel_conversation_id) WHERE channel_conversation_id IS NOT NULL;

-- Create composite indexes for common queries
CREATE INDEX idx_conversations_tenant_status ON conversations(tenant_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_tenant_channel ON conversations(tenant_id, channel) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_tenant_assigned ON conversations(tenant_id, assigned_user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_conversations_unassigned ON conversations(tenant_id, status) WHERE assigned_user_id IS NULL AND is_deleted = FALSE;
CREATE INDEX idx_conversations_active ON conversations(tenant_id, status, last_message_at) WHERE status IN ('OPEN', 'IN_PROGRESS', 'PENDING') AND is_deleted = FALSE;

-- Add constraints
ALTER TABLE conversations ADD CONSTRAINT check_conversations_channel 
    CHECK (channel IN ('WHATSAPP', 'TELEGRAM', 'MESSENGER', 'INSTAGRAM', 'EMAIL', 'WEBCHAT', 'SMS', 'TWITTER', 'API'));

ALTER TABLE conversations ADD CONSTRAINT check_conversations_status 
    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'PENDING', 'RESOLVED', 'CLOSED', 'SPAM', 'ARCHIVED'));

ALTER TABLE conversations ADD CONSTRAINT check_conversations_priority 
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'));

ALTER TABLE conversations ADD CONSTRAINT check_conversations_satisfaction_rating 
    CHECK (satisfaction_rating IS NULL OR (satisfaction_rating >= 1 AND satisfaction_rating <= 5));

-- Foreign key constraints
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE conversations ADD CONSTRAINT fk_conversations_contact_id 
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE;

ALTER TABLE conversations ADD CONSTRAINT fk_conversations_assigned_user_id 
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Create trigger for updated_at
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();