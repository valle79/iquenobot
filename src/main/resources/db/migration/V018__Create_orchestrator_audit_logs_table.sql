-- Create orchestrator_audit_logs table
CREATE TABLE orchestrator_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    message_id VARCHAR(255),
    conversation_id VARCHAR(255),
    contact_id VARCHAR(255),
    channel VARCHAR(20) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    action_type VARCHAR(50),
    decision_strategy VARCHAR(100),
    processing_time_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    error_type VARCHAR(100),
    input_data TEXT,
    output_data TEXT,
    metadata TEXT,
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for common queries
CREATE INDEX idx_audit_logs_tenant_id ON orchestrator_audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_message_id ON orchestrator_audit_logs(message_id) WHERE message_id IS NOT NULL;
CREATE INDEX idx_audit_logs_conversation_id ON orchestrator_audit_logs(conversation_id) WHERE conversation_id IS NOT NULL;
CREATE INDEX idx_audit_logs_contact_id ON orchestrator_audit_logs(contact_id) WHERE contact_id IS NOT NULL;
CREATE INDEX idx_audit_logs_channel ON orchestrator_audit_logs(channel);
CREATE INDEX idx_audit_logs_event_type ON orchestrator_audit_logs(event_type);
CREATE INDEX idx_audit_logs_action_type ON orchestrator_audit_logs(action_type) WHERE action_type IS NOT NULL;
CREATE INDEX idx_audit_logs_status ON orchestrator_audit_logs(status);
CREATE INDEX idx_audit_logs_created_at ON orchestrator_audit_logs(created_at);

-- Create composite indexes for complex queries
CREATE INDEX idx_audit_logs_tenant_event ON orchestrator_audit_logs(tenant_id, event_type);
CREATE INDEX idx_audit_logs_tenant_channel ON orchestrator_audit_logs(tenant_id, channel);
CREATE INDEX idx_audit_logs_tenant_status ON orchestrator_audit_logs(tenant_id, status);
CREATE INDEX idx_audit_logs_tenant_date ON orchestrator_audit_logs(tenant_id, created_at DESC);
CREATE INDEX idx_audit_logs_conversation_date ON orchestrator_audit_logs(conversation_id, created_at DESC) 
    WHERE conversation_id IS NOT NULL;

-- Index for failed operations query
CREATE INDEX idx_audit_logs_failed ON orchestrator_audit_logs(tenant_id, status, created_at DESC) 
    WHERE status = 'FAILED';

-- Add constraints
ALTER TABLE orchestrator_audit_logs ADD CONSTRAINT check_audit_logs_status 
    CHECK (status IN ('SUCCESS', 'FAILED', 'PARTIAL', 'PENDING'));

ALTER TABLE orchestrator_audit_logs ADD CONSTRAINT check_audit_logs_event_type 
    CHECK (event_type IN ('MESSAGE_RECEIVED', 'DECISION_MADE', 'ACTION_EXECUTED', 
                          'PROCESSING_ERROR', 'CONVERSATION_CREATED', 'AGENT_ASSIGNED',
                          'BOT_ANSWERED', 'CONVERSATION_CLOSED'));

-- Foreign key constraint
ALTER TABLE orchestrator_audit_logs ADD CONSTRAINT fk_audit_logs_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Create trigger for updated_at
CREATE TRIGGER update_audit_logs_updated_at BEFORE UPDATE ON orchestrator_audit_logs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create partitioning by date for better performance (optional, for large volumes)
-- This can be implemented later when needed:
-- CREATE TABLE orchestrator_audit_logs_2024_01 PARTITION OF orchestrator_audit_logs
--     FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
